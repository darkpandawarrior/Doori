package com.mileway

import com.mileway.core.platform.ShareSheet

class FakeShareSheet : ShareSheet {
    override fun share(text: String, subject: String?, fileUri: String?) {}
}
