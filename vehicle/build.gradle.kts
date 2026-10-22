plugins {
    id("com.android.application")
}

android {
    namespace = "app.majung.vehicle"
    compileSdk = 34
    defaultConfig {
        applicationId = "app.majung.vehicle"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-dev"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { buildConfig = true }
    buildTypes {
        debug {
            buildConfigField("String", "PICKUP_API", "\"http://10.0.2.2:8080\"")
        }
        release {
            // A production endpoint and authentication are deliberately not configured yet.
            buildConfigField("String", "PICKUP_API", "\"\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("ai.pleos.playground:NaviHelper:3.0.5")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("junit:junit:4.13.2")
}
