plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace =
        "com.renato.launcher.baselineprofile"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 29
        targetSdk = 37

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"
}

baselineProfile {
    /*
     * Generate using our physical Samsung
     * connected through ADB/usbipd.
     *
     * Android 13+ supports profile capture on
     * a non-rooted physical device.
     */
    useConnectedDevices = true
}

dependencies {
    implementation(
        libs.androidx.benchmark.macro.junit4
    )

    implementation(
        libs.androidx.test.ext.junit.current
    )

    implementation(
        libs.androidx.test.uiautomator
    )
}
