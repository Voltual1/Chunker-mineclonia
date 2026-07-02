plugins {
    `java-library` 
}
java.sourceCompatibility = JavaVersion.VERSION_21

dependencies {
    // Originally this was 'compileOnly' and shaded inside 'tasks.jar' to keep the jar clean.
    // However, when this project is included as a local project dependency in an Android app, 
    // Android's variant matching ignores the custom 'tasks.jar' output and causes 'NoClassDefFoundError' at runtime.
    // Changing this to 'implementation' ensures Android Gradle Plugin (AGP) properly passes down the dependency chain.
    implementation(project(":bedrock-pack-schema")) 
    
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains:annotations:24.0.1")
}

// NOTE: The custom tasks.jar shading logic has been removed because we now use 'implementation'
// above to support Android's dependency resolution mechanism.

/*tasks.jar {
    from(bedrockPackSchemaSourceSet.output)
    duplicatesStrategy = DuplicatesStrategy.WARN
}*/