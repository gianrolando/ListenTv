plugins { id("com.android.application") }
android {
    namespace = "it.gianrolando.listentv"
    compileSdk = 35
    defaultConfig {
        applicationId = "it.gianrolando.listentv"
        minSdk = 23
        targetSdk = 35
        versionCode = 12
        versionName = "1.3.3"
    }
    buildTypes {
        getByName("release") { isDebuggable = false; isMinifyEnabled = false }
    }
    testOptions { unitTests.isIncludeAndroidResources = true }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("net.sf.kxml:kxml2:2.3.0")
    implementation("org.tukaani:xz:1.10")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    val media3 = "1.8.0"
    implementation("androidx.media3:media3-exoplayer:$media3")
    implementation("androidx.media3:media3-exoplayer-hls:$media3")
    implementation("androidx.media3:media3-exoplayer-dash:$media3")
    implementation("androidx.media3:media3-session:$media3")
    implementation("com.google.guava:guava:33.4.0-android")
}
