plugins {
    `java-library`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

sourceSets {
    named("main") {
        java.srcDir("libs/manifest-editor/lib/src/main/java")
        resources.srcDir("libs/manifest-editor/lib/src/main")
    }
}

tasks.processResources {
    exclude("**/*.java")
}
