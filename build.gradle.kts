// Root build — applies no plugins itself; subprojects declare their own.
// Version catalog is in gradle/libs.versions.toml and is available to all subprojects.
plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
}
