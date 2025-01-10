plugins {
    kotlin("jvm") version "2.1.0"
}

group = "me.emyar"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.jedis)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotest)
    testImplementation(libs.junitParams)
    testImplementation(libs.testContainers)
    testImplementation(libs.testContainersJunit)
    testImplementation(libs.testContainersRedis)
}

kotlin {
    jvmToolchain(21)

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
