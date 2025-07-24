
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
    implementation(group= "ch.qos.logback", name= "logback-classic", version= "1.5.17")
    implementation(group= "ch.qos.logback", name= "logback-core", version= "1.5.17")
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")
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
    implementation("me.friwi:jcefmaven:132.3.1")
    implementation("me.friwi:jcef-natives-linux-amd64:jcef-1770317+cef-132.3.1+g144febe+chromium-132.0.6834.83")
    implementation("me.friwi:jogl-all:v2.4.0-rc-20210111")
    implementation("me.friwi:gluegen-rt:v2.4.0-rc-20210111")

    //Web UI for GUI
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.9.1")
    //Special snowflake web reactivity library we wrote
    val shadeVersion = "0.5.16"
    implementation("net.justmachinery:shade:$shadeVersion")
    //Web server
    implementation("io.javalin:javalin:6.6.0")
    implementation("net.justmachinery.futility:futility-core:1.0.5") //General utility

    val serializationVersion = "1.9.0"
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$serializationVersion")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
        sourceCompatibility = JavaVersion.VERSION_24
        targetCompatibility = JavaVersion.VERSION_24
    }
}
kotlin {
    jvmToolchain(24)
}
tasks.named<UpdateDaemonJvm>("updateDaemonJvm") {
    languageVersion = JavaLanguageVersion.of(24)
}

tasks.withType<JavaCompile> {
    sourceCompatibility = JavaVersion.VERSION_23.toString()
    targetCompatibility = JavaVersion.VERSION_23.toString()
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_23)
        progressiveMode = true
        compilerOptions.freeCompilerArgs.add("-Xcontext-parameters")
    }
}

tasks.withType(JavaExec::class).configureEach {
    javaLauncher.set(javaToolchains.launcherFor(java.toolchain))
}