package com.miletracker

import com.miletracker.core.platform.ShareSheet

class FakeShareSheet : ShareSheet {
    override fun share(text: String, subject: String?, fileUri: String?) {}
}
