plugins {
    kotlin("jvm") version "2.1.0"
}

group = "me.emyar"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation(libs.jedis)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotest)
    testImplementation(libs.testContainers)
    testImplementation(libs.testContainersJunit)
    testImplementation(libs.testContainersRedis)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks {
    wrapper {
        gradleVersion = "8.11.1"
    }

    test {
        useJUnitPlatform()
    }
}
