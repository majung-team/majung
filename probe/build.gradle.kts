plugins {
    id("com.android.application")
}

android {
    namespace = "app.majung.probe"
    compileSdk = 34

    defaultConfig {
        applicationId = "app.majung.probe"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-probe"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("ai.pleos.playground:NaviHelper:3.0.5")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("junit:junit:4.13.2")
}
