import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.JavaVersion.VERSION_1_8
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.withType
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.jooq.util.GenerationTool
import org.jooq.util.jaxb.*
import org.jooq.util.jaxb.Target
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.flywaydb.gradle.FlywayExtension
import org.slf4j.LoggerFactory

val hopperVersion by project
val kotlinVersion by project

buildscript {
    val kotlinBuildScriptVersion = "1.1.51"

    repositories {
        jcenter()
        mavenCentral()
        maven(url = "https://plugins.gradle.org/m2/")
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinBuildScriptVersion")
        classpath("com.github.jengelman.gradle.plugins:shadow:1.2.3")
        classpath("nu.studer:gradle-jooq-plugin:2.0.7")
        classpath("org.xerial:sqlite-jdbc:3.16.1")
        classpath("gradle.plugin.com.boxfuse.client:flyway-release:4.2.0")
    }

}

plugins {
    kotlin("jvm") version "1.1.51"
    application
}

apply {
    plugin("com.github.johnrengelman.shadow")
    plugin("jacoco")
    plugin("maven-publish")
    plugin("nu.studer.jooq")
    plugin("org.flywaydb.flyway")
}

java {
    sourceCompatibility = VERSION_1_8
    targetCompatibility = VERSION_1_8

    sourceSets["main"].java.srcDirs.add(File("src/generated/java"))
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

    compile("com.google.guava:guava:22.0")

    compile("com.sparkjava:spark-core:2.6.0")

    compile("chat.willow.warren:Warren:2.1.0.5")

    compile("com.squareup.moshi:moshi:1.5.0")
    compile("com.squareup.moshi:moshi-kotlin:1.5.0")

    compile("org.xerial:sqlite-jdbc:3.16.1")

    compile("org.jooq:jooq")
    compile("org.flywaydb:flyway-core:4.2.0")

    testCompile("junit:junit:4.12")
    testCompile("org.mockito:mockito-core:2.7.21")
    testCompile("com.nhaarman:mockito-kotlin:1.4.0")
}

task(name = "generateJooq") {
    val configuration = Configuration().apply {
        jdbc = Jdbc().apply {
            driver = "org.sqlite.JDBC"
            url = "jdbc:sqlite:run/hopper.db"
        }

        generator = Generator().apply {
            name = "org.jooq.util.DefaultGenerator"
            strategy = Strategy().apply {
                name = "org.jooq.util.DefaultGeneratorStrategy"
            }
            target = Target().apply {
                packageName = "chat.willow.hopper.generated"
                directory = "src/main/java/"
            }
            database = Database().apply {
                name = "org.jooq.util.sqlite.SQLiteDatabase"
                includes = ".*"
                excludes = ""

            }
        }
    }

    if (File("run/hopper.db").exists()) {
        GenerationTool.generate(configuration)
    } else {
        LoggerFactory.getLogger("generateJooq").warn("run/hopper.db doesn't exist, so not running JOOQ generation")
    }
}

configure<JacocoPluginExtension> {
    toolVersion = "0.7.9"
}

tasks.withType<JacocoReport> {
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

val shadowJars = tasks.withType<ShadowJar> {
    mergeServiceFiles()

    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")

    manifest.attributes.apply {
        put("Main-Class", mainClass)
    }
}

configure<FlywayExtension> {
    driver = "org.sqlite.JDBC"
    url = "jdbc:sqlite:run/hopper.db"
    locations = arrayOf("filesystem:src/main/resources/db/migration")
}

val sourcesJar = task<Jar>("sourcesJar") {
    dependsOn("classes")

    from(java.sourceSets["main"].allSource)
    classifier = "sources"
}

project.artifacts.add("archives", sourcesJar)
project.artifacts.add("archives", shadowJars.first())

configure<PublishingExtension> {
    val deployUrl = if (project.hasProperty("DEPLOY_URL")) { project.property("DEPLOY_URL") } else { project.buildDir.absolutePath }
    this.repositories.maven({ setUrl("$deployUrl") })

    publications {
        create<MavenPublication>("mavenJava") {
            from(components.getByName("java"))

            artifact(sourcesJar)
            artifact(shadowJars.first())

            artifactId = projectTitle
        }
    }
}

fun kotlin(module: String, version: String = kotlinVersion as String) = "org.jetbrains.kotlin:kotlin-$module:$version"

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.apiVersion = "1.1"
}