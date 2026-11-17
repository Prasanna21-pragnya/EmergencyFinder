
plugins {
    alias(libs.plugins.android.application) apply false

    // Firebase Google Services plugin
    id("com.google.gms.google-services") version "4.6.0" apply false

    // KSP plugin for Room database
    id("com.google.devtools.ksp") version "2.2.10-2.0.2" apply false
}
