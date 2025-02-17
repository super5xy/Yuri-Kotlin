@file:Suppress("SpellCheckingInspection")

group = "com.mikuac"

plugins {
    kotlin("jvm") version "1.8.22"
    kotlin("plugin.jpa") version "1.8.22"
    kotlin("plugin.spring") version "1.8.22"
    kotlin("plugin.allopen") version "1.8.22"

    id("io.freefair.lombok") version "8.0.1"
    // id("org.graalvm.buildtools.native") version "0.9.19"
    id("org.springframework.boot") version "3.1.0"
    id("io.spring.dependency-management") version "1.1.0"
}

java.sourceCompatibility = JavaVersion.VERSION_17

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()
    }
    // 不注释这个 native 编译找不到启动类
    jar {
        enabled = false
    }
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    api("org.springframework.boot:spring-boot-starter")
    api("org.springframework.boot:spring-boot-starter-aop")
    api("org.springframework.boot:spring-boot-starter-jdbc")
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("org.springframework.boot:spring-boot-starter-validation")

    api("com.mikuac:shiro:2.0.5")
    api("net.jodah:expiringmap:0.5.10")
    api("cn.hutool:hutool-core:5.8.19")
    api("cn.hutool:hutool-system:5.8.20")
    api("net.coobird:thumbnailator:0.4.19")
    api("com.github.oshi:oshi-core:6.4.3")
    api("org.telegram:telegrambots:6.7.0")
    api("org.jsoup:jsoup:1.16.1")
    api("com.squareup.okhttp3:okhttp:4.11.0")
    api("com.kennycason:kumo-core:1.28")
    api("com.huaban:jieba-analysis:1.0.2")
    api("com.theokanning.openai-gpt3-java:service:0.12.0")
    api("com.google.code.gson:gson:2.10.1")
    api("org.graalvm.js:js:22.3.2")
    api("org.graalvm.js:js-scriptengine:22.3.2")
    api("com.microsoft.playwright:playwright:1.35.0")
    api("mysql:mysql-connector-java:8.0.33")
    api("org.xerial:sqlite-jdbc:3.42.0.0")
    api("org.hibernate.orm:hibernate-community-dialects")

    api("org.jetbrains.kotlin:kotlin-reflect:1.8.22")
    api("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.22")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
}