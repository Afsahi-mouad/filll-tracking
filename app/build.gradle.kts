import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.filltracking2"
    compileSdk = 35

    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { localProperties.load(it) }
    }

    defaultConfig {
        applicationId = "com.example.filltracking2"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "3.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

// Updated signing config
    signingConfigs {
        create("release") {
            localProperties.getProperty("STORE_FILE")?.let {
                storeFile = file(it)
            }
            storePassword = localProperties.getProperty("STORE_PASSWORD")
            keyAlias = localProperties.getProperty("KEY_ALIAS")
            keyPassword = localProperties.getProperty("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.datastore)
    implementation(libs.material)
    
    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    
    // Jetpack Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2026.04.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    
    // Compose UI
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    
    // Material 3
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")
    
    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    
    // Icons
    implementation("androidx.compose.material:material-icons-extended")
    
    // Coil for images
    implementation("io.coil-kt:coil-compose:2.5.0")
    
    // Gson for JSON serialization (lightweight persistence)
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Charts
    implementation("com.patrykandpatrick.vico:compose:2.0.0-alpha.15")
    implementation("com.patrykandpatrick.vico:compose-m3:2.0.0-alpha.15")
    
    // Calendar
    implementation(libs.compose.calendar)
    
    // Excel Export (Apache POI)
    implementation("org.apache.poi:poi-ooxml:5.2.5")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}