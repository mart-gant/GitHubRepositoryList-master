plugins {
    java
    id("org.springframework.boot") version "4.0.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.marcingantkowski"
version = "0.0.1-SNAPSHOT"
description = "GitHubRepositoryLister"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-restclient")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.wiremock:wiremock-standalone:3.10.0")
    testImplementation("org.apache.commons:commons-lang3:3.19.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
