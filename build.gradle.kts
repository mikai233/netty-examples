plugins {
    java
    kotlin("jvm") version "1.5.10"
    scala
}

group = "com.mdreamfever"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    implementation("org.bouncycastle", "bcpkix-jdk15on", "1.68")
    implementation("io.netty", "netty-all", "4.1.65.Final")
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.5.0")
    implementation("org.scala-lang", "scala-library", "2.13.6")
    implementation("com.sun.activation", "jakarta.activation", "2.0.1")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}