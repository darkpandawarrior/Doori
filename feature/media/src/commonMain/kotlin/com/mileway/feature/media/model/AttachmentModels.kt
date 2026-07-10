package com.mileway.feature.media.model

/**
 * Camera flash mode, cycled by the in-camera flash toggle.
 *
 * The order [AUTO] -> [ON] -> [OFF] mirrors the cycle of the on-screen toggle and the
 * underlying CameraX `ImageCapture.FLASH_MODE_*` constants the controller is configured with.
 */
enum class FlashMode { AUTO, ON, OFF }

// AttachmentItem/OcrResult/UploadState/AttachmentSource moved to :core:media (V25 P25.A1.1) so every
// consumer can reach them without depending on feature:media. Re-exported here via typealias so
// existing `com.mileway.feature.media.model.*` imports keep compiling unchanged.
typealias AttachmentSource = com.mileway.core.media.model.AttachmentSource
typealias OcrResult = com.mileway.core.media.model.OcrResult
typealias UploadState = com.mileway.core.media.model.UploadState
typealias AttachmentItem = com.mileway.core.media.model.AttachmentItem
