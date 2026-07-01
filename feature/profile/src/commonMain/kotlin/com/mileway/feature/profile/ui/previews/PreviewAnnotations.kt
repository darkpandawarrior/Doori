package com.mileway.feature.profile.ui.previews

import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "Phone · Light", showBackground = true)
annotation class PhonePreview

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = 0x20)
annotation class LightDarkPreview
