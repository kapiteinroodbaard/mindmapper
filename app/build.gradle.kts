import com.android.build.api.artifact.SingleArtifact
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// The version is defined once as `app.version` in gradle.properties (Semantic Versioning).
// Minor and patch are limited to 0-99 so the derived versionCode always increases.
val appVersion: String = providers.gradleProperty("app.version").get().trim()
val appVersionCode: Int = run {
    val match = Regex("""(0|[1-9]\d*)\.(0|[1-9]\d?)\.(0|[1-9]\d?)""").matchEntire(appVersion)
        ?: throw GradleException("app.version '$appVersion' must be MAJOR.MINOR.PATCH with minor and patch 0-99")
    val (major, minor, patch) = match.destructured.toList().map { it.toInt() }
    if (major > 20_000) throw GradleException("app.version major '$major' is too large for a versionCode")
    major * 10_000 + minor * 100 + patch
}

// Machine-specific settings (never committed): APK copy destination and release signing.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) file.inputStream().use { load(it) }
}
val apkOutputDir: String? = localProperties.getProperty("apk.outputDir")?.takeIf { it.isNotBlank() }
val signingStoreFile: String? = localProperties.getProperty("signing.storeFile")?.takeIf { it.isNotBlank() }

android {
    namespace = "io.github.wlenaerts.mindmapper"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.wlenaerts.mindmapper"
        minSdk = 35
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersion
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (signingStoreFile != null) {
            create("local") {
                storeFile = rootProject.file(signingStoreFile)
                storePassword = localProperties.getProperty("signing.storePassword")
                keyAlias = localProperties.getProperty("signing.keyAlias")
                keyPassword = localProperties.getProperty("signing.keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Unsigned unless a local keystore is configured; F-Droid signs with its own key.
            signingConfig = signingConfigs.findByName("local")
            vcsInfo.include = false
        }
    }

    // F-Droid rejects the encrypted dependency metadata blob.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    androidResources {
        generateLocaleConfig = true
        // Only ship (and offer in the per-app language settings) the languages the app is translated into.
        localeFilters += listOf("en", "nl")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        error += setOf("MissingTranslation", "HardcodedText", "ExtraTranslation")
        abortOnError = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core"))

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(composeBom)
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}

/**
 * Copies the variant's APK as `mindmapper-v<version>[-debug].apk` into `build/outputs/mindmapper/`
 * and, when `apk.outputDir` is set in local.properties, into that folder too (overwriting).
 */
abstract class CopyNamedApkTask : DefaultTask() {
    @get:InputFiles
    abstract val apkDirectory: DirectoryProperty

    @get:Internal
    abstract val builtArtifactsLoader: Property<com.android.build.api.variant.BuiltArtifactsLoader>

    @get:Input
    abstract val apkName: Property<String>

    @get:OutputDirectory
    abstract val localOutputDir: DirectoryProperty

    @get:Input
    @get:Optional
    abstract val externalOutputDir: Property<String>

    @TaskAction
    fun copy() {
        val artifacts = builtArtifactsLoader.get().load(apkDirectory.get())
            ?: throw GradleException("No APK found in ${apkDirectory.get()}")
        val apk = File(artifacts.elements.single().outputFile)
        val local = localOutputDir.get().file(apkName.get()).asFile
        apk.copyTo(local, overwrite = true)
        logger.lifecycle("APK: ${local.absolutePath}")
        externalOutputDir.orNull?.let { dir ->
            val target = File(dir, apkName.get())
            apk.copyTo(target, overwrite = true)
            logger.lifecycle("APK copied to ${target.absolutePath}")
        }
    }
}

androidComponents {
    onVariants { variant ->
        val suffix = if (variant.buildType == "release") "" else "-${variant.name}"
        val taskName = "copy${variant.name.replaceFirstChar { it.uppercase() }}Apk"
        val copyTask = tasks.register<CopyNamedApkTask>(taskName) {
            apkDirectory.set(variant.artifacts.get(SingleArtifact.APK))
            builtArtifactsLoader.set(variant.artifacts.getBuiltArtifactsLoader())
            apkName.set("mindmapper-v$appVersion$suffix.apk")
            localOutputDir.set(layout.buildDirectory.dir("outputs/mindmapper"))
            if (apkOutputDir != null) externalOutputDir.set(apkOutputDir)
        }
        val variantName = variant.name.replaceFirstChar { it.uppercase() }
        tasks.matching { it.name == "assemble$variantName" }.configureEach { finalizedBy(copyTask) }

        // Privacy guardrail: the release manifest must not request network or dangerous permissions.
        if (variant.buildType == "release") {
            val checkTask = tasks.register<CheckPermissionsTask>("check${variantName}Permissions") {
                mergedManifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
            }
            tasks.named("check") { dependsOn(checkTask) }
        }
    }
}

/** Fails when the merged manifest requests a forbidden permission (see spec `privacy`). */
abstract class CheckPermissionsTask : DefaultTask() {
    @get:InputFile
    abstract val mergedManifest: RegularFileProperty

    @TaskAction
    fun check() {
        val forbidden = setOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.ACCESS_WIFI_STATE",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.READ_MEDIA_IMAGES",
            "android.permission.CAMERA",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.READ_CONTACTS",
            "android.permission.RECORD_AUDIO",
            "android.permission.POST_NOTIFICATIONS",
        )
        val requested = Regex("""<uses-permission[^>]*android:name="([^"]+)"""")
            .findAll(mergedManifest.get().asFile.readText())
            .map { it.groupValues[1] }
            .toSet()
        val violations = requested intersect forbidden
        if (violations.isNotEmpty()) {
            throw GradleException("Forbidden permissions in release manifest: ${violations.sorted().joinToString()}")
        }
    }
}
