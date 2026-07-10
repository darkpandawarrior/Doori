package com.mileway.core.media.watermark

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGContextRestoreGState
import platform.CoreGraphics.CGContextSaveGState
import platform.CoreGraphics.CGContextScaleCTM
import platform.CoreGraphics.CGContextSetTextPosition
import platform.CoreGraphics.CGContextTranslateCTM
import platform.CoreGraphics.CGFloatVar
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.CoreText.CTLineCreateWithAttributedString
import platform.CoreText.CTLineDraw
import platform.CoreText.CTLineGetTypographicBounds
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSAttributedString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.UIKit.NSFontAttributeName
import platform.UIKit.NSForegroundColorAttributeName
import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererContext
import platform.UIKit.UIImage
import platform.UIKit.UIRectFill

/**
 * iOS actual (V26 P26.WM.2): draws a semi-transparent legibility strip across the bottom-right of
 * the image and burns [text] onto it (white, bold) via `UIGraphicsImageRenderer` + Core Text —
 * `UIKit`'s `NSString`/`NSAttributedString` drawing category (`sizeWithAttributes`/`drawAtPoint`)
 * isn't included in this project's UIKit cinterop bindings (`objcClassesIncludingCategories` in the
 * platform `UIKit.def` only whitelists a few paragraph-style classes), so text is measured and
 * drawn with Core Text's `CTLine` directly instead.
 *
 * Falls back to returning [imageUri] unchanged on any decode/encode failure — watermarking must
 * never block a capture.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual suspend fun burnWatermark(
    imageUri: String,
    text: String,
): String {
    val path = imageUri.removePrefix("file://")
    val image = UIImage.imageWithContentsOfFile(path) ?: return imageUri
    val size = image.size
    val renderer = UIGraphicsImageRenderer(size = size)
    val data =
        renderer.JPEGDataWithCompressionQuality(0.92, actions = { context ->
            image.drawAtPoint(CGPointMake(0.0, 0.0))
            if (context != null) drawLegibilityStripAndText(context, text, size)
        })

    val outPath = NSTemporaryDirectory() + "watermark_${NSUUID().UUIDString}.jpg"
    val written = data.writeToFile(outPath, atomically = true)
    return if (written) "file://$outPath" else imageUri
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun drawLegibilityStripAndText(
    rendererContext: UIGraphicsImageRendererContext,
    text: String,
    size: CValue<CGSize>,
) {
    val (w, h) = size.useContents { width to height }
    val fontSize = w * 0.035
    val padding = fontSize * 0.6
    val stripHeight = fontSize + padding * 2.0

    UIColor.colorWithWhite(0.0, alpha = 0.55).setFill()
    UIRectFill(CGRectMake(0.0, h - stripHeight, w, stripHeight))

    val attrs =
        mapOf<Any?, Any?>(
            NSFontAttributeName to UIFont.boldSystemFontOfSize(fontSize),
            NSForegroundColorAttributeName to UIColor.whiteColor,
        )
    val attrString = NSAttributedString.create(string = text, attributes = attrs)
    // CTLineCreateWithAttributedString takes a toll-free-bridged CFAttributedStringRef, not the
    // NSAttributedString ObjC wrapper directly — this project's CoreText/Foundation cinterop
    // bindings need the explicit CFBridgingRetain/reinterpret hop to cross that boundary.
    val cfAttrString = CFBridgingRetain(attrString)
    val line = CTLineCreateWithAttributedString(cfAttrString?.reinterpret())
    val lineWidth =
        memScoped {
            val ascent = alloc<CGFloatVar>()
            val descent = alloc<CGFloatVar>()
            val leading = alloc<CGFloatVar>()
            CTLineGetTypographicBounds(line, ascent.ptr, descent.ptr, leading.ptr)
        }

    val ctx = rendererContext.CGContext
    val textX = w - padding - lineWidth
    val textY = h - padding - fontSize * 0.8 // approx cap-height offset above the strip's baseline

    CGContextSaveGState(ctx)
    // UIGraphicsImageRenderer's context is Y-flipped to match UIKit; Core Text always draws in a
    // Y-up space relative to the CTM, so undo the flip locally around the text's own origin before
    // drawing — the standard recipe for mixing Core Text into a UIGraphicsImageRenderer context.
    CGContextTranslateCTM(ctx, textX, textY)
    CGContextScaleCTM(ctx, 1.0, -1.0)
    CGContextSetTextPosition(ctx, 0.0, 0.0)
    CTLineDraw(line, ctx)
    CGContextRestoreGState(ctx)
    CFBridgingRelease(cfAttrString)
}
