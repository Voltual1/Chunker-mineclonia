import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

plugins {
    id("io.freefair.lombok") version "9.5.0"
    `java-library`
}

sourceSets {
    main {
        resources {
            srcDir("src/main/java/resources")
        }
    }
}

java.sourceCompatibility = JavaVersion.VERSION_21

abstract class PatchIIOUtilTransform : TransformAction<TransformParameters.None> {
    @get:InputParameters
    abstract val parameters: TransformParameters.None

    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val inputFile = inputArtifact.get().asFile
        val outputFile = outputs.file(inputFile.name)

        if (inputFile.name.contains("imageio-core-3.9.4")) {
            ZipFile(inputFile).use { zipIn ->
                ZipOutputStream(FileOutputStream(outputFile)).use { zipOut ->
                    val entries = zipIn.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        
                        if (entry.name == "com/twelvemonkeys/imageio/util/IIOUtil.class") {
                            zipOut.putNextEntry(ZipEntry(entry.name))
                            
                            val patchedFile = File("${System.getProperty("user.dir")}/patches/IIOUtil.class")
                            if (patchedFile.exists()) {
                                zipOut.write(patchedFile.readBytes())
                            } else {
                                throw GradleException("找不到修改后的 IIOUtil.class，请确保路径正确: ${patchedFile.absolutePath}")
                            }
                            
                            zipOut.closeEntry()
                        } else {
                            zipOut.putNextEntry(ZipEntry(entry.name))
                            zipIn.getInputStream(entry).use { it.copyTo(zipOut) }
                            zipOut.closeEntry()
                        }
                    }
                }
            }
            println("====== 已替换 Jar 中的 IIOUtil.class ======")
        } else {
            inputFile.copyTo(outputFile, overwrite = true)
        }
    }
}

dependencies {
    registerTransform(PatchIIOUtilTransform::class) {
        from.attribute(Attribute.of("artifactType", String::class.java), "jar")
        to.attribute(Attribute.of("artifactType", String::class.java), "jar")
    }
}

dependencies {
    api(project(":pack-schema-api"))
    compileOnly(project(":bedrock-pack-schema")) // Is provided by pack-schema-api for consumers, but not for us during compile time
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("commons-io:commons-io:2.11.0")
    
    implementation("com.twelvemonkeys.imageio:imageio-tga:3.9.4")
    
    implementation("it.unimi.dsi:fastutil:8.5.18")
    api("net.kyori:adventure-api:4.14.0")
    api("net.kyori:adventure-text-serializer-gson:4.14.0")
    api("net.kyori:adventure-text-serializer-legacy:4.14.0")
    api(project(":creative-api"))
    api("team.unnamed:creative-serializer-minecraft:1.13.6") {
        exclude(group = "team.unnamed", module = "creative-api")
    }
    api(libs.guava)
    compileOnly("com.google.auto.service:auto-service:1.0.1")
    annotationProcessor("com.google.auto.service:auto-service:1.0.1")
}

java {
    withSourcesJar()
}