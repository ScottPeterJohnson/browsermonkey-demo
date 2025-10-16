
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    application
    val kotlinVersion = System.getProperties().getProperty("kotlinVersion")
    kotlin("jvm").version(kotlinVersion)
    id("com.github.ben-manes.versions").version("0.52.0")  //For finding outdated dependencies
    id("idea")
    kotlin("plugin.serialization").version(kotlinVersion)
}

application {
    group = "net.justmachinery.browsermonkey"
    version = "1.0-SNAPSHOT"
    mainClass = "net.justmachinery.browsermonkey.MainKt"
}

repositories {
    mavenCentral()
}

dependencies {
    val kotlinVersion = System.getProperties().getProperty("kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

    //Logging
    implementation(group= "ch.qos.logback", name= "logback-classic", version= "1.5.18")
    implementation(group= "ch.qos.logback", name= "logback-core", version= "1.5.18")
    implementation("net.logstash.logback:logstash-logback-encoder:8.1")
    implementation("org.slf4j:slf4j-api:2.1.0-alpha1")
    implementation("org.slf4j:jcl-over-slf4j:2.1.0-alpha1")
    implementation("org.slf4j:jul-to-slf4j:2.1.0-alpha1")
    implementation("io.github.microutils:kotlin-logging:3.0.2")

    val jmeVersion = "3.8.0-stable"
    implementation("org.jmonkeyengine:jme3-core:$jmeVersion")
    implementation("org.jmonkeyengine:jme3-desktop:$jmeVersion")
    implementation("org.jmonkeyengine:jme3-lwjgl3:$jmeVersion")
    implementation("org.jmonkeyengine:jme3-jogg:$jmeVersion")
    implementation("org.jmonkeyengine:jme3-effects:$jmeVersion")
    implementation("org.jmonkeyengine:jme3-jbullet:$jmeVersion")
    implementation("org.jmonkeyengine:jme3-terrain:$jmeVersion")
    implementation("org.jmonkeyengine:jme3-testdata:$jmeVersion")
    implementation("org.jmonkeyengine:jme3-plugins:$jmeVersion")

    //Browser-based GUI
    implementation("me.friwi:jcefmaven:135.0.20")
    implementation("me.friwi:jcef-natives-linux-amd64:jcef-ca49ada+cef-135.0.20+ge7de5c3+chromium-135.0.7049.85")
    implementation("me.friwi:jcef-natives-windows-amd64:jcef-ca49ada+cef-135.0.20+ge7de5c3+chromium-135.0.7049.85")

    //Web UI for GUI
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.12.0")
    //Special snowflake web reactivity library we wrote
    val shadeVersion = "0.6.0"
    implementation("net.justmachinery:shade:$shadeVersion")
    //Web server
    implementation("io.javalin:javalin:6.7.0")
    implementation("net.justmachinery.futility:futility-core:1.0.5") //General utility

    val serializationVersion = "1.9.0"
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$serializationVersion")
}


java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }
}
kotlin {
    jvmToolchain(25)
}
tasks.named<UpdateDaemonJvm>("updateDaemonJvm") {
    languageVersion = JavaLanguageVersion.of(25)
}

tasks.withType<JavaCompile> {
    sourceCompatibility = JavaVersion.VERSION_23.toString()
    targetCompatibility = JavaVersion.VERSION_23.toString()
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_23)
        progressiveMode = true
        compilerOptions {
            freeCompilerArgs.add("-Xcontext-parameters")
            optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
        }
    }
}

tasks.withType(JavaExec::class).configureEach {
    javaLauncher.set(javaToolchains.launcherFor(java.toolchain))
}

gradle.taskGraph.whenReady {
    val task = this.allTasks.find { it.name.endsWith(".main()") } as? JavaExec
    task?.let {
        it.setExecutable(it.javaLauncher.get().executablePath.asFile.absolutePath)
    }
}