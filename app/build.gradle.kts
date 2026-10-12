plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "de.titanyus.antplusexplorer"
    compileSdk = 35
    defaultConfig {
        applicationId = "de.titanyus.antplusexplorer"
        minSdk = 33
        targetSdk = 35
        versionCode = 3
        versionName = "0.1.2"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(files("libs/antpluginlib_3-9-0.aar"))
    testImplementation("junit:junit:4.13.2")
}
