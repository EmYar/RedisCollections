plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
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
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation(libs.jedis)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation(libs.testContainers)
    testImplementation(libs.testContainersRedis)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks {
    wrapper {
        gradleVersion = "8.10.2"
    }

    test {
        useJUnitPlatform()
    }
}
