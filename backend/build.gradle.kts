plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group   = "com.communitybot"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

// Lombok annotation processor must be on both compile and test paths
configurations {
    compileOnly { extendsFrom(configurations.annotationProcessor.get()) }
}

dependencies {
    implementation(platform(libs.langchain4j.bom))
    implementation(platform(libs.langgraph4j.bom))
    implementation(libs.langgraph4j.core)
    implementation(libs.langgraph4j.agent.executor)
    implementation(libs.langgraph4j.langchain4j)
    implementation(libs.langchain4j)
    implementation(libs.langchain4j.open.ai)
    implementation(libs.langchain4j.web.search.tavily)

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.oauth2.client)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.spring.boot.starter.websocket)
    implementation(libs.spring.boot.starter.actuator)

    implementation(libs.flyway.core)
    runtimeOnly(libs.flyway.postgresql)
    runtimeOnly(libs.postgresql)

    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)

    implementation(libs.minio)
    implementation(libs.tika.core)
    implementation(libs.pdfbox)
    implementation(libs.poi.ooxml)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.testcontainers.junit5)
    testImplementation(libs.testcontainers.postgresql)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

/** Load infra/.env so `./gradlew :backend:bootRun` works without manually sourcing env vars. */
fun loadInfraDotEnv(): Map<String, String> {
    val envFile = rootProject.projectDir.resolve("infra/.env")
    if (!envFile.isFile) return emptyMap()
    return envFile.readLines()
        .mapNotNull { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) return@mapNotNull null
            val eq = trimmed.indexOf('=')
            if (eq <= 0) return@mapNotNull null
            trimmed.substring(0, eq).trim() to trimmed.substring(eq + 1).trim()
        }
        .toMap()
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    jvmArgs = listOf("-Xmx512m", "-Xms128m")
    val dotenv = loadInfraDotEnv()
    dotenv.forEach { (key, value) -> environment(key, value) }
    val profile = System.getenv("SPRING_PROFILES_ACTIVE") ?: dotenv["SPRING_PROFILES_ACTIVE"]
    if (profile != null && !profile.isBlank()) {
        args("--spring.profiles.active=$profile")
    }
}
