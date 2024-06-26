plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    kotlin("plugin.serialization")
    kotlin("kapt")
    id ("com.mikepenz.aboutlibraries.plugin")
}

android {
    namespace = "com.example.mymusic"
    compileSdk = 34



    defaultConfig {
        applicationId = "com.example.mymusic"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

//        ksp {
//            arg("room.schemaLocation", "$projectDir/schemas")
//        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
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
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.datastore.core)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0-alpha01")


    //Shimmer
    implementation("com.facebook.shimmer:shimmer:0.5.0")

    //Legacy Support


    implementation("com.android.support:recyclerview-v7:21.0.0")

    implementation("com.daimajia.swipelayout:library:1.2.0@aar")

    //coil
    implementation("io.coil-kt:coil:2.5.0")
    //Palette
    implementation("androidx.palette:palette-ktx:1.0.0")

    //ExoPlayer
    val media3_version = "1.2.1"

    implementation("androidx.media3:media3-exoplayer:$media3_version")
    implementation("androidx.media3:media3-ui:$media3_version")
    implementation("androidx.media3:media3-session:$media3_version")
    implementation("androidx.media3:media3-exoplayer-dash:$media3_version")
    implementation("androidx.media3:media3-exoplayer-hls:$media3_version")
    implementation("androidx.media3:media3-exoplayer-rtsp:$media3_version")
    implementation("androidx.media3:media3-exoplayer-smoothstreaming:$media3_version")
    implementation("androidx.media3:media3-exoplayer-workmanager:$media3_version")
    implementation("androidx.media3:media3-datasource-okhttp:$media3_version")

    //Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    ksp("com.google.dagger:hilt-compiler:2.50")
    ksp("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.8.0")
    //Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.7.3")
//DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    val ktor_version = "2.3.8"

    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-okhttp:$ktor_version")
    implementation("io.ktor:ktor-client-encoding:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-xml:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-protobuf:$ktor_version")

    implementation("org.brotli:dec:0.1.2")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    //Easy Permissions
    implementation("pub.devrel:easypermissions:3.0.0")

    //Custom Activity On Crash
    implementation ("cat.ereza:customactivityoncrash:2.4.0")
    implementation("com.google.code.gson:gson:2.10.1")

    //Insetter
    implementation("dev.chrisbanes.insetter:insetter:0.6.1")
    implementation("dev.chrisbanes.insetter:insetter-dbx:0.6.1")

    //Lottie
    val lottieVersion = "6.3.0"
    implementation("com.airbnb.android:lottie:$lottieVersion")

    //Paging 3
    val paging_version= "3.2.1"
    implementation("androidx.paging:paging-runtime-ktx:$paging_version")
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation("com.github.skydoves:balloon:1.6.4")

    val latestAboutLibsRelease = "10.10.0"
    implementation ("com.mikepenz:aboutlibraries:${latestAboutLibsRelease}")

    val version = "0.3.1"
    // For parsing HTML
    implementation("com.mohamedrejeb.ksoup:ksoup-html:$version")

// Only for encoding and decoding HTML entities
    implementation("com.mohamedrejeb.ksoup:ksoup-entities:0.3.1")
}

hilt {
    enableAggregatingTask = true
}