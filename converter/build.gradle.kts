plugins {
    `java-library`
    id("io.freefair.lombok") version "9.5.0"
}

version = "Based PackConverter-1ae5b8f048de39d6074132f44d58824c305eb295"

sourceSets {
    main {
        resources {
            srcDir("src/main/java/resources")
        }
    }
}

interface PatchParameters : TransformParameters {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    val patchFiles: ConfigurableFileCollection
}

abstract class PatchIIOUtilTransform : TransformAction<PatchParameters> {

    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val inputFile = inputArtifact.get().asFile
        val outputFile = outputs.file(inputFile.name)

        if (inputFile.name.contains("imageio-core-3.9.4")) {
            val patchDir = parameters.patchFiles.files
            val patchedFile = patchDir.firstOrNull { it.name == "IIOUtil.class" }
                ?: throw GradleException("在指定的补丁路径中找不到 IIOUtil.class")

            ZipFile(inputFile).use { zipIn ->
                ZipOutputStream(FileOutputStream(outputFile)).use { zipOut ->
                    val entries = zipIn.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        
                        if (entry.name == "com/twelvemonkeys/imageio/util/IIOUtil.class") {
                            zipOut.putNextEntry(ZipEntry(entry.name))
                            zipOut.write(patchedFile.readBytes())
                            zipOut.closeEntry()
                        } else {
                            zipOut.putNextEntry(ZipEntry(entry.name))
                            zipIn.getInputStream(entry).use { it.copyTo(zipOut) }
                            zipOut.closeEntry()
                        }
                    }
                }
            }
            println("已替换 Jar 中的 IIOUtil.class")
        } else {
            inputFile.copyTo(outputFile, overwrite = true)
        }
    }
}

dependencies {
    api(project(":pack-schema-api"))
    compileOnly(project(":bedrock-pack-schema")) // Is provided by pack-schema-api for consumers, but not for us during compile time
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("commons-io:commons-io:2.11.0")
    implementation("com.twelvemonkeys.imageio:imageio-tga:3.9.4") {
    exclude(group = "com.twelvemonkeys.imageio", module = "imageio-core")
}
    implementation("it.unimi.dsi:fastutil:8.5.18")
    api("net.kyori:adventure-api:4.14.0")
    api("net.kyori:adventure-text-serializer-gson:4.14.0")
    api("net.kyori:adventure-text-serializer-legacy:4.14.0")
    api(project(":creative-api"))
    api("team.unnamed:creative-serializer-minecraft:1.14.4") {
        exclude(group = "team.unnamed", module = "creative-api")
    }

    compileOnly("com.google.auto.service:auto-service:1.0.1")
    annotationProcessor("com.google.auto.service:auto-service:1.0.1")
}

java {
    withSourcesJar()
}
