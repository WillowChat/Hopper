import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.JavaVersion.VERSION_1_8
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.tasks.Jar
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

val hopperVersion by project
val kotlinVersion by project

buildscript {

    val buildscriptKotlinVersion = "1.1.1"
    fun kotlin(module: String, version: String = buildscriptKotlinVersion) = "org.jetbrains.kotlin:kotlin-$module:$version"

    repositories {
        gradleScriptKotlin()
        jcenter()
    }

    dependencies {
        classpath(kotlin("gradle-plugin"))
        classpath("com.github.jengelman.gradle.plugins:shadow:1.2.3")
    }

}

plugins {
    application
}

apply {
    plugin("kotlin")
    plugin("com.github.johnrengelman.shadow")
    plugin("jacoco")
    plugin("maven-publish")
}

java {
    sourceCompatibility = VERSION_1_8
    targetCompatibility = VERSION_1_8
}

val buildNumberAddition = if(project.hasProperty("BUILD_NUMBER")) { ".${project.property("BUILD_NUMBER")}" } else { "" }

version = "$hopperVersion$buildNumberAddition"
group = "chat.willow.hopper"
val projectTitle = "Hopper"
val mainClass = "$group.$projectTitle"

project.setProperty("archivesBaseName", projectTitle)

application {
    mainClassName = mainClass
}

repositories {
    jcenter()
    gradleScriptKotlin()
    maven { setUrl("https://maven.ci.carrot.codes/") }
    maven { setUrl("https://oss.sonatype.org/content/repositories/snapshots") }
    maven { setUrl("https://dl.bintray.com/kotlin/exposed") }
}

dependencies {
    compile(kotlin("stdlib"))

    compile("org.slf4j:slf4j-api:1.7.25")
    compile("org.slf4j:slf4j-simple:1.7.25")

    compile("com.sparkjava:spark-core:2.5.5")

    compile("chat.willow.warren:Warren:2.1.0.5")

    compile("org.pac4j:spark-pac4j:2.0.0-RC2")
    compile("org.pac4j:pac4j-http:2.0.0-RC2")

    compile("com.squareup.moshi:moshi:1.5.0-SNAPSHOT")
    compile("com.squareup.moshi:moshi-kotlin:1.5.0-SNAPSHOT")

    compile("org.xerial:sqlite-jdbc:3.16.1")

    compile("org.jetbrains.exposed:exposed:0.7.6")

    testCompile("junit:junit:4.12")
    testCompile("org.mockito:mockito-core:2.7.21")
    testCompile("com.nhaarman:mockito-kotlin:1.4.0")
}

configure<JacocoPluginExtension> {
    toolVersion = "0.7.9"
}

val jacocoTestReport: JacocoReport by tasks
jacocoTestReport.apply {
    classDirectories = fileTree("build/classes/main").apply {
        // Exclude well known data classes that should contain no logic
        // Remember to change values in codecov.yml too
        exclude("**/*Event.*")
        exclude("**/*State.*")
        exclude("**/*Configuration.*")
        exclude("**/*Runner.*")
    }

    reports.xml.isEnabled = true
    reports.html.isEnabled = true
}

val sourceSets = java.sourceSets
val mainSourceSet = sourceSets["main"]

val shadowJar: ShadowJar by tasks
shadowJar.apply {
    mergeServiceFiles()

    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")

    manifest.attributes.apply {
        put("Main-Class", mainClass)
    }
}

val sourcesJar = task<Jar>("sourcesJar") {
    dependsOn("classes")

    from(mainSourceSet.allSource)
    classifier = "sources"
}

project.artifacts.add("archives", sourcesJar)
project.artifacts.add("archives", shadowJar)

configure<PublishingExtension> {
    val deployUrl = if (project.hasProperty("DEPLOY_URL")) { project.property("DEPLOY_URL") } else { project.buildDir.absolutePath }
    this.repositories.maven({ setUrl("$deployUrl") })

    publications {
        create<MavenPublication>("mavenJava") {
            from(components.getByName("java"))

            artifact(sourcesJar)
            artifact(shadowJar)

            artifactId = projectTitle
        }
    }
}


fun kotlin(module: String, version: String = kotlinVersion as String) = "org.jetbrains.kotlin:kotlin-$module:$version"