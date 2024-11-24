import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.getmap"
    compileSdk = 33

    android.buildFeatures.buildConfig = true

    defaultConfig {
        applicationId = "com.example.getmap"
        minSdk = 26
        targetSdk = 33
        versionCode = 29
        versionName = "2.2.13"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }

        buildConfigField("String", "USERNAME", "\"${getPropertyFromFile("USERNAME")}\"")
        buildConfigField("String", "PASSWORD", "\"${getPropertyFromFile("PASSWORD")}\"")
        buildConfigField("String", "AW_USER_NAME", "\"${getPropertyFromFile("AW_USER_NAME")}\"")
        buildConfigField("String", "AW_PASSWORD", "\"${getPropertyFromFile("AW_PASSWORD")}\"")
        buildConfigField("String", "AW_API", "\"${getPropertyFromFile("AW_API")}\"")
        buildConfigField("String", "AIRWATCH_TENANT", "\"${getPropertyFromFile("AIRWATCH_TENANT")}\"")
        buildConfigField("String", "BASE_URL", "\"${getPropertyFromFile("BASE_URL")}\"")
        buildConfigField("String", "DEPLOY_ENV", "\"${findProperty("deployEnv") ?: "pub"}\"")

    }

    signingConfigs {
        getByName("debug") {
            val homeDir = System.getenv("USERPROFILE") ?: System.getenv("HOME")
            storeFile = file("$homeDir/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }

        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        dataBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.3"
    }

    packaging {
        resources {
            excludes += "/META-INF/*"
        }
    }


    applicationVariants.configureEach{
        val variant = this
        variant.outputs.configureEach {
            val variantOutputImpl = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val deployEnv = findProperty("deployEnv") ?: "pub"
            variantOutputImpl.outputFileName ="cloud-mapping-${deployEnv}-${variant.versionName}-${variant.name}.apk"
        }
    }
    dependencies{
        implementation( "com.github.matomo-org:matomo-sdk-android:4.2")
    }
}

dependencies {
    implementation("com.github.DImuthuUpe:AndroidPdfViewer:2.8.1")
//    implementation("com.esri.arcgisruntime:arcgis-android:100.10.0")
    implementation("com.esri:arcgis-maps-kotlin:200.3.0")
    implementation("com.github.NASAWorldWind:WorldWindAndroid:v0.8.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0-alpha01")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation(project(mapOf("path" to ":sdk")))
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.13.0")
    implementation("com.squareup.moshi:moshi-adapters:1.13.0")
    implementation("androidx.room:room-runtime:2.5.2")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation(files("libs/AirWatchSDK-23.07.aar"))
    implementation(files("libs/FeatureModule-android-2.0.2.aar"))
    implementation(files("libs/sdk-fm-extension-android-2.0.2.aar"))
    implementation(files("libs/ws1-android-logger-23.07.aar"))

    implementation( "com.github.matomo-org:matomo-sdk-android:4.2")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.2")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.2")

    implementation ("androidx.core:core-ktx:1.7.0")
    implementation ("com.github.matomo-org:matomo-sdk-android:4.1.4")


    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.activity:activity-compose:1.7.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2023.03.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.preference:preference:1.2.0")
    implementation(project(":sdk"))
    implementation(project(":sdk"))
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.03.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

fun getPropertyFromFile(propertyName: String): String {
    val props = Properties()
    val file = rootProject.file("secrets.properties")
    FileInputStream(file).use { props.load(it) }
    return props.getProperty(propertyName)
}
