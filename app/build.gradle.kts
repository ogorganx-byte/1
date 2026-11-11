plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.buildfinance.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.buildfinance.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}
