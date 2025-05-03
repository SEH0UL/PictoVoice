plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Dependencias para los servicios de google
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.pictovoice"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.pictovoice"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Dependencias básicas de Android
    implementation(libs.androidx.core.ktx)
    implementation (libs.androidx.appcompat.v161)
    implementation (libs.material.v190)
    implementation(libs.androidx.activity)
    implementation (libs.androidx.constraintlayout.v214)

    // Firebase BoM (Bill of Materials) - Controla las versiones automáticamente
    implementation(platform(libs.firebase.bom.v3270)) // Última versión estable

    // Dependencias de Firebase (sin versión explícita, BoM la maneja)
    implementation (libs.com.google.firebase.firebase.auth.ktx)
    implementation (libs.google.firebase.firestore.ktx)
    implementation (libs.google.firebase.common.ktx)

    // Dependencias para ViewModel y LiveData
    implementation (libs.androidx.lifecycle.viewmodel.ktx.v262)
    implementation (libs.androidx.lifecycle.livedata.ktx)
    implementation (libs.androidx.lifecycle.runtime.ktx.v262)

    // Dependencias para corrutinas
    implementation (libs.kotlinx.coroutines.android)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

}