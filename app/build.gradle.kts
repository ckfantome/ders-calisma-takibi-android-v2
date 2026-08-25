plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.derscalismatakibi.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.derscalismatakibi.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 23
        versionName = "0.23.0"

        // Buyuk assetler (yuz landmark modeli) sikistirilmadan paketlensin -
        // aksi halde MediaPipe calisirken asset'i acmakta sorun yasayabilir.
        androidResources {
            noCompress += "task"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    composeOptions {
        // Kotlin 1.9.22 ile eslesen Compose Compiler surumu.
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // com.sun.mail:android-mail ve android-activation ayni META-INF
            // dosyalarini (NOTICE.md, LICENSE.txt vb.) tasiyor, cakisiyor.
            excludes += "/META-INF/NOTICE.md"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/LICENSE.md"
        }
    }
}

// Room semasini app/schemas altina JSON olarak disa aktarir - gelecekteki
// versiyon atlamalarinda gercek Migration yazabilmek icin gereklidir
// (bkz. data/AppDatabase.kt -> exportSchema = true).
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // --- Compose ---
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    // LifecycleService - StudyForegroundService icin (arkaplanda kamera takibi).
    implementation("androidx.lifecycle:lifecycle-service:2.7.0")
    implementation("androidx.navigation:navigation-compose:2.7.6")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // --- CameraX ---
    val cameraxVersion = "1.3.1"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // --- MediaPipe Tasks Vision (FaceLandmarker) ---
    implementation("com.google.mediapipe:tasks-vision:0.10.14")

    // --- OpenCV (solvePnP ile masaustundekiyle AYNI kafa pozu hesaplamasi icin) ---
    implementation("org.opencv:opencv:4.9.0")

    // --- Room (SQLite - masaustundeki 'sessions' tablosunun karsiligi) ---
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // --- DataStore (Ayarlar - masaustundeki config.json karsiligi) ---
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // --- Kotlin coroutines ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    implementation("androidx.core:core-ktx:1.12.0")

    // --- WorkManager (gunluk yedekleme/e-posta zamanlamasi) ---
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // --- JavaMail (SMTP ile gunluk yedek e-postasi) ---
    implementation("com.sun.mail:android-mail:1.6.8")
    implementation("com.sun.mail:android-activation:1.6.8")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
