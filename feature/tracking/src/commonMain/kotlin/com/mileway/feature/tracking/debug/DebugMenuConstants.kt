package com.mileway.feature.tracking.debug

/**
 * Constants used throughout the debug menu to ensure consistent behavior
 */
object DebugMenuConstants {
    /**
     * List of options that require an app restart when changed
     */
    val RESTART_REQUIRED_OPTIONS =
        listOf(
            "Force UAT",
            "Force Prod",
            "Force Dev Environment",
            "Force Custom Origin",
            "Force MENA Login",
            "Skip OTP Pin Screen",
            "Force Track Miles V2 UI",
        )

    /**
     * List of mutually exclusive options
     * When one is enabled, others should be disabled
     */
    val MUTUALLY_EXCLUSIVE_OPTION_GROUPS =
        mapOf(
            "API_ORIGIN" to
                listOf(
                    "Force UAT",
                    "Force Prod",
                    "Force Dev Environment",
                    "Force Custom Origin",
                ),
        )

    /**
     * Default values for custom fields
     */
    val DEFAULT_CUSTOM_VALUES =
        mapOf(
            "Custom Origin" to "https://api.example.com",
            "Custom OTP" to "123456",
            "Custom Banner" to "Debug Mode Active",
        )

    /**
     * Option descriptions to improve usability
     */
    val OPTION_DESCRIPTIONS =
        mapOf(
            "Force UAT" to "Switches backend environment to UAT. May require restart.",
            "Force Prod" to "Switches backend environment to Production. May require restart.",
            "Force Dev Environment" to "Switches backend environment to Development. May require restart.",
            "Force Custom Origin" to "Allows setting a custom API origin URL. May require restart.",
            "Force MENA Login" to "Forces MENA region login screen.",
            "Skip OTP Pin Screen" to "Skips OTP pin screen for faster login.",
            "Force Track Miles V2 UI" to "Enables Track Miles V2 user interface.",
            "Custom Origin" to "Set a custom base API URL for requests.",
            "Custom OTP" to "Set a custom OTP for login/debug.",
            "Custom Banner" to "Custom banner displayed in debug mode.",
        )

    /**
     * Help text for the debug menu
     */
    const val HELP_TEXT =
        "The Debug Menu allows you to configure various settings for testing and development purposes. " +
            "Use the profiles for quick configuration or toggle individual options as needed. " +
            "Note that some options require an app restart to take effect."
}
