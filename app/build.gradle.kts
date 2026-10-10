import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import javax.xml.parsers.DocumentBuilderFactory

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.ksp)
}

group = "com.tristinbaker.idlefantasy"
version = "1.15.7"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
}

val generatedDesktopDir = layout.buildDirectory.dir("generated/desktop/kotlin")

sourceSets {
    main {
        kotlin.srcDir("src/main/kotlin")
        kotlin.srcDir(generatedDesktopDir)
        resources.srcDir("src/main/assets")
    }
    test {
        kotlin.srcDir("src/test/kotlin")
    }
}

val generateDesktopSources by tasks.registering {
    val resDir = file("src/main/res")
    val outDir = generatedDesktopDir
    inputs.dir(resDir)
    outputs.dir(outDir)

    doLast {
        val pkgDir = outDir.get().asFile.resolve("com/fantasyidler")
        pkgDir.mkdirs()

        // 1. Generate BuildConfig.kt
        pkgDir.resolve("BuildConfig.kt").writeText(
            """
            package com.fantasyidler

            object BuildConfig {
                const val DEBUG: Boolean = true
                const val APPLICATION_ID: String = "com.tristinbaker.idlefantasy"
                const val BUILD_TYPE: String = "debug"
                const val VERSION_CODE: Int = 157000
                const val VERSION_NAME: String = "1.15.7"
            }
            """.trimIndent() + "\n"
        )

        // 2. Collect strings, plurals, arrays from res/values/*.xml
        val stringNames = sortedSetOf<String>()
        val pluralNames = sortedSetOf<String>()
        val arrayNames = sortedSetOf<String>()
        val dbf = DocumentBuilderFactory.newInstance()

        resDir.listFiles { f -> f.isDirectory && f.name.startsWith("values") }?.forEach { valDir ->
            valDir.listFiles { f -> f.isFile && f.extension == "xml" }?.forEach { xmlFile ->
                try {
                    val doc = dbf.newDocumentBuilder().parse(xmlFile)
                    val root = doc.documentElement
                    val children = root.childNodes
                    for (i in 0 until children.length) {
                        val node = children.item(i)
                        val attrName = node.attributes?.getNamedItem("name")?.nodeValue ?: continue
                        when (node.nodeName) {
                            "string" -> stringNames.add(attrName)
                            "plurals" -> pluralNames.add(attrName)
                            "string-array" -> arrayNames.add(attrName)
                        }
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to parse resource XML ${xmlFile.path}: ${e.message}")
                }
            }
        }

        // 3. Collect drawables from res/drawable*
        val drawableNames = sortedSetOf<String>()
        resDir.listFiles { f -> f.isDirectory && f.name.startsWith("drawable") }?.forEach { drawDir ->
            drawDir.listFiles { f -> f.isFile }?.forEach { df ->
                val baseName = df.name.substringBeforeLast('.')
                if (baseName.matches(Regex("[a-zA-Z_][a-zA-Z0-9_]*"))) {
                    drawableNames.add(baseName)
                }
            }
        }

        val sb = StringBuilder()
        sb.appendLine("package com.fantasyidler")
        sb.appendLine()
        sb.appendLine("object R {")

        // object string
        sb.appendLine("    object string {")
        stringNames.forEachIndexed { index, name ->
            val id = 0x7f010001 + index
            sb.appendLine("        const val `$name`: Int = $id")
        }
        sb.appendLine("    }")

        // object plurals
        sb.appendLine("    object plurals {")
        pluralNames.forEachIndexed { index, name ->
            val id = 0x7f020001 + index
            sb.appendLine("        const val `$name`: Int = $id")
        }
        sb.appendLine("    }")

        // object array
        sb.appendLine("    object array {")
        arrayNames.forEachIndexed { index, name ->
            val id = 0x7f030001 + index
            sb.appendLine("        const val `$name`: Int = $id")
        }
        sb.appendLine("    }")

        // object drawable
        sb.appendLine("    object drawable {")
        drawableNames.forEachIndexed { index, name ->
            val id = 0x7f040001 + index
            sb.appendLine("        const val `$name`: Int = $id")
        }
        sb.appendLine("    }")

        // Lookup maps via reflection over const val fields (avoids 64KB JVM method bytecode limit)
        sb.appendLine("    val stringIdToName: Map<Int, String> by lazy {")
        sb.appendLine("        string::class.java.declaredFields")
        sb.appendLine("            .filter { it.type == Int::class.javaPrimitiveType }")
        sb.appendLine("            .associate { it.getInt(null) to it.name }")
        sb.appendLine("    }")
        sb.appendLine("    val stringNameToId: Map<String, Int> by lazy {")
        sb.appendLine("        stringIdToName.entries.associate { (k, v) -> v to k }")
        sb.appendLine("    }")
        sb.appendLine("    val pluralsIdToName: Map<Int, String> by lazy {")
        sb.appendLine("        plurals::class.java.declaredFields")
        sb.appendLine("            .filter { it.type == Int::class.javaPrimitiveType }")
        sb.appendLine("            .associate { it.getInt(null) to it.name }")
        sb.appendLine("    }")
        sb.appendLine("    val arrayIdToName: Map<Int, String> by lazy {")
        sb.appendLine("        array::class.java.declaredFields")
        sb.appendLine("            .filter { it.type == Int::class.javaPrimitiveType }")
        sb.appendLine("            .associate { it.getInt(null) to it.name }")
        sb.appendLine("    }")
        sb.appendLine("    val drawableIdToName: Map<Int, String> by lazy {")
        sb.appendLine("        drawable::class.java.declaredFields")
        sb.appendLine("            .filter { it.type == Int::class.javaPrimitiveType }")
        sb.appendLine("            .associate { it.getInt(null) to it.name }")
        sb.appendLine("    }")
        sb.appendLine("    val drawableNameToId: Map<String, Int> by lazy {")
        sb.appendLine("        drawableIdToName.entries.associate { (k, v) -> v to k }")
        sb.appendLine("    }")
        sb.appendLine("}")
        pkgDir.resolve("R.kt").writeText(sb.toString())
    }
}

tasks.named<Copy>("processResources") {
    from("src/main/res") {
        into("res")
    }
}

tasks.named("compileKotlin") {
    dependsOn(generateDesktopSources)
}

tasks.matching { it.name == "kspKotlin" || it.name == "kspTestKotlin" }.configureEach {
    dependsOn(generateDesktopSources)
}

// Export Room schema files so migrations can be verified and reviewed in version control.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

dependencies {
    // Compose Desktop
    implementation(compose.desktop.currentOs)
    implementation(compose.ui)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.components.resources)

    // Lifecycle & Navigation Multiplatform (JVM Desktop)
    implementation(libs.jetbrains.lifecycle.viewmodel.compose)
    implementation(libs.jetbrains.lifecycle.runtime.compose)
    implementation(libs.jetbrains.navigation.compose)

    // Room JVM + Bundled SQLite Driver
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.sqlite.bundled)
    add("ksp", libs.androidx.room.compiler)

    // Coroutines + Serialization + Inject + Reflection (for lightweight DI)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.javax.inject)
    implementation(libs.kotlin.reflect)

    // Unit testing (JVM)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)
}

compose.desktop {
    application {
        mainClass = "com.fantasyidler.DesktopMainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Dmg)
            packageName = "IdleFantasy"
            packageVersion = "1.15.7"
        }
    }
}

