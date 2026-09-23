#!/usr/bin/swift
//
// generate_brand_icons.swift
//
// P4.7: one source mark -> every icon size the watch (and wear) targets need, so
// watch/widget/app icons stay in sync with Doori's brand mark without hand-exporting PNGs.
// Approach mirrors biciradar's tooling/project/generate_brand_icons.swift (public, MIT):
// draw the mark once with Core Graphics, rasterize at every required size, emit Contents.json.
//
// The mark itself is Doori's own (route trail + map pin over a blue gradient) — it is the
// same glyph already shipped as app/src/main/res/drawable/ic_launcher_{foreground,background}.xml,
// just redrawn here in Core Graphics so one script can regenerate every platform's raster set.
//
// Usage: swift tooling/generate_brand_icons.swift
// Regenerating reproduces byte-identical PNGs for a given size (deterministic draw, no randomness).

import CoreGraphics
import Foundation
import ImageIO
import UniformTypeIdentifiers

let repoRoot = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()

// MARK: - Drawing the mark

/// Draws the Doori brand mark (blue gradient square + white route/pin glyph) into `size` px.
func drawMark(size: Int) -> CGImage {
    let ctx = CGContext(
        data: nil,
        width: size,
        height: size,
        bitsPerComponent: 8,
        bytesPerRow: 0,
        space: CGColorSpaceCreateDeviceRGB(),
        bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
    )!
    let s = CGFloat(size) / 108.0 // source viewport is 108x108 (matches the Android adaptive-icon vectors)

    // Background: same diagonal gradient as ic_launcher_background.xml (#2196F3 -> #0B3D91).
    let colors = [
        CGColor(srgbRed: 33.0 / 255, green: 150.0 / 255, blue: 243.0 / 255, alpha: 1), // #2196F3
        CGColor(srgbRed: 11.0 / 255, green: 61.0 / 255, blue: 145.0 / 255, alpha: 1), // #0B3D91
    ] as CFArray
    let gradient = CGGradient(colorsSpace: CGColorSpaceCreateDeviceRGB(), colors: colors, locations: [0, 1])!
    ctx.drawLinearGradient(
        gradient,
        start: CGPoint(x: 6 * s, y: CGFloat(size) - 6 * s),
        end: CGPoint(x: 102 * s, y: CGFloat(size) - 102 * s),
        options: []
    )

    // Foreground glyph: route trail + start marker + map pin (same path data as ic_launcher_foreground.xml),
    // flipped into CG's bottom-left origin space.
    func flip(_ x: CGFloat, _ y: CGFloat) -> CGPoint { CGPoint(x: x * s, y: CGFloat(size) - y * s) }

    ctx.setStrokeColor(CGColor(srgbRed: 1, green: 1, blue: 1, alpha: 1))
    ctx.setLineWidth(7 * s)
    ctx.setLineCap(.round)
    ctx.beginPath()
    ctx.move(to: flip(54, 69))
    ctx.addCurve(to: flip(38, 81), control1: flip(49, 78), control2: flip(44, 80))
    ctx.strokePath()

    ctx.setFillColor(CGColor(srgbRed: 1, green: 1, blue: 1, alpha: 1))
    ctx.fillEllipse(in: CGRect(x: (37 - 4.5) * s, y: CGFloat(size) - (81 + 4.5) * s, width: 9 * s, height: 9 * s))

    ctx.beginPath()
    ctx.move(to: flip(54, 24))
    ctx.addCurve(to: flip(72, 42), control1: flip(63.94, 24), control2: flip(72, 32.06))
    ctx.addCurve(to: flip(54, 72), control1: flip(72, 53), control2: flip(64, 61))
    ctx.addCurve(to: flip(36, 42), control1: flip(44, 61), control2: flip(36, 53))
    ctx.addCurve(to: flip(54, 24), control1: flip(36, 32.06), control2: flip(44.06, 24))
    ctx.closePath()
    ctx.addEllipse(in: CGRect(x: (47.5 - 6.5) * s, y: CGFloat(size) - (40 + 6.5) * s, width: 13 * s, height: 13 * s))
    ctx.drawPath(using: .eoFill)

    return ctx.makeImage()!
}

func writePNG(_ image: CGImage, to url: URL) {
    guard let dest = CGImageDestinationCreateWithURL(url as CFURL, UTType.png.identifier as CFString, 1, nil) else {
        fatalError("Could not create PNG destination at \(url.path)")
    }
    CGImageDestinationAddImage(dest, image, nil)
    guard CGImageDestinationFinalize(dest) else {
        fatalError("Could not write PNG at \(url.path)")
    }
}

func generate(sizePt: Double, scale: Int, to url: URL) {
    let px = Int((sizePt * Double(scale)).rounded())
    writePNG(drawMark(size: px), to: url)
}

// MARK: - watchOS AppIcon.appiconset (MilewayWatch target)

// Sizes/idioms per Apple's watchOS icon spec (notification/companion/app-launcher/quick-look).
struct IconSpec {
    let sizePt: Double
    let scale: Int
    let idiom: String
    let role: String?
    let subtype: String?
}

let watchIcons: [IconSpec] = [
    IconSpec(sizePt: 22, scale: 2, idiom: "watch", role: "notificationCenter", subtype: "38mm"),
    IconSpec(sizePt: 24, scale: 2, idiom: "watch", role: "notificationCenter", subtype: "42mm"),
    IconSpec(sizePt: 27.5, scale: 2, idiom: "watch", role: "notificationCenter", subtype: "45mm"),
    IconSpec(sizePt: 29, scale: 2, idiom: "watch", role: "companionSettings", subtype: nil),
    IconSpec(sizePt: 29, scale: 3, idiom: "watch", role: "companionSettings", subtype: nil),
    IconSpec(sizePt: 33, scale: 2, idiom: "watch", role: "notificationCenter", subtype: "49mm"),
    IconSpec(sizePt: 40, scale: 2, idiom: "watch", role: "appLauncher", subtype: "38mm"),
    IconSpec(sizePt: 44, scale: 2, idiom: "watch", role: "appLauncher", subtype: "40mm"),
    IconSpec(sizePt: 46, scale: 2, idiom: "watch", role: "appLauncher", subtype: "41mm"),
    IconSpec(sizePt: 50, scale: 2, idiom: "watch", role: "appLauncher", subtype: "44mm"),
    IconSpec(sizePt: 51, scale: 2, idiom: "watch", role: "appLauncher", subtype: "45mm"),
    IconSpec(sizePt: 54, scale: 2, idiom: "watch", role: "appLauncher", subtype: "49mm"),
    IconSpec(sizePt: 86, scale: 2, idiom: "watch", role: "quickLook", subtype: "38mm"),
    IconSpec(sizePt: 98, scale: 2, idiom: "watch", role: "quickLook", subtype: "42mm"),
    IconSpec(sizePt: 108, scale: 2, idiom: "watch", role: "quickLook", subtype: "44mm"),
    IconSpec(sizePt: 117, scale: 2, idiom: "watch", role: "quickLook", subtype: "45mm"),
    IconSpec(sizePt: 129, scale: 2, idiom: "watch", role: "quickLook", subtype: "49mm"),
    IconSpec(sizePt: 1024, scale: 1, idiom: "watch-marketing", role: nil, subtype: nil),
]

func generateWatchIconSet() {
    let appIconSet = repoRoot
        .appendingPathComponent("iosApp/MilewayWatch/Assets.xcassets/AppIcon.appiconset")
    try? FileManager.default.createDirectory(at: appIconSet, withIntermediateDirectories: true)

    var images: [[String: Any]] = []
    for spec in watchIcons {
        let scaleSuffix = spec.idiom == "watch-marketing" ? "" : "@\(spec.scale)x"
        let subtypeSuffix = spec.subtype.map { "-\($0)" } ?? ""
        let roleSuffix = spec.role.map { "-\($0)" } ?? ""
        let filename = "icon-\(Int(spec.sizePt.rounded(.down)))x\(Int(spec.sizePt.rounded(.down)))\(subtypeSuffix)\(roleSuffix)\(scaleSuffix).png"
        generate(sizePt: spec.sizePt, scale: spec.scale, to: appIconSet.appendingPathComponent(filename))

        var entry: [String: Any] = [
            "filename": filename,
            "idiom": spec.idiom,
            "scale": "\(spec.scale)x",
            "size": "\(formatSize(spec.sizePt))x\(formatSize(spec.sizePt))",
        ]
        if let role = spec.role { entry["role"] = role }
        if let subtype = spec.subtype { entry["subtype"] = subtype }
        images.append(entry)
    }

    let contents: [String: Any] = [
        "images": images,
        "info": ["author": "xcode", "version": 1],
    ]
    writeJSON(contents, to: appIconSet.appendingPathComponent("Contents.json"))

    // Assets.xcassets needs its own top-level Contents.json too.
    let catalogRoot = appIconSet.deletingLastPathComponent()
    writeJSON(
        ["info": ["author": "xcode", "version": 1]],
        to: catalogRoot.appendingPathComponent("Contents.json")
    )
}

func formatSize(_ v: Double) -> String {
    v.truncatingRemainder(dividingBy: 1) == 0 ? String(Int(v)) : String(format: "%.1f", v)
}

func writeJSON(_ dict: [String: Any], to url: URL) {
    let data = try! JSONSerialization.data(withJSONObject: dict, options: [.prettyPrinted, .sortedKeys])
    try! data.write(to: url)
}

// MARK: - Android :wear launcher mipmaps (no adaptive-icon vector support gap noted in P4.7)

let wearMipmaps: [(dir: String, px: Int)] = [
    ("mipmap-mdpi", 48),
    ("mipmap-hdpi", 72),
    ("mipmap-xhdpi", 96),
    ("mipmap-xxhdpi", 144),
    ("mipmap-xxxhdpi", 192),
]

func generateWearLauncherIcons() {
    let resRoot = repoRoot.appendingPathComponent("wear/src/main/res")
    for entry in wearMipmaps {
        let dir = resRoot.appendingPathComponent(entry.dir)
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        writePNG(drawMark(size: entry.px), to: dir.appendingPathComponent("ic_launcher.png"))
    }
}

generateWatchIconSet()
generateWearLauncherIcons()
print("Generated watchOS AppIcon.appiconset (\(watchIcons.count) images) + :wear launcher mipmaps (\(wearMipmaps.count) densities).")
