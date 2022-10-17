plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    api("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.30")
    api("org.jetbrains.dokka:dokka-gradle-plugin:1.4.10.2")
    api("com.gradle.publish:plugin-publish-plugin:0.12.0")
    api("com.github.gmazzo:gradle-buildconfig-plugin:2.0.2")
}
