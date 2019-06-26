import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer
import no.nils.wsdl2java.Wsdl2JavaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "no.nav"
version = "0.8.3-SNAPSHOT"

val kotlinVersion = "1.3.31"
val ktorVersion = "1.2.1"
val jacksonVersion = "2.9.8"

val prometheusVersion = "0.6.0"
val kotlinloggingVersion = "1.6.22"
val logstashEncoderVersion = "5.3"
val logbackVersion = "1.2.3"

val konfigVersion = "1.6.10.0"

val jaxwsVersion = "2.3.1"
val jaxwsToolsVersion = "2.3.1"
val javaxActivationVersion = "1.1.1"
val cxfVersion = "3.3.1"
val unboundidVersion = "4.0.8"

val swaggerVersion = "3.1.7"

// test dependencies
val kluentVersion = "1.47"
val spekVersion = "2.0.0"

val mainClass = "no.nav.altinn.admin.BootstrapKt"

plugins {
    application
    java
    kotlin("jvm") version "1.3.31"
    id("no.nils.wsdl2java") version "0.10"
    id("org.jmailen.kotlinter") version "1.26.0"
    id("com.github.ben-manes.versions") version "0.20.0"
    id("com.github.johnrengelman.shadow") version "4.0.4"
    id("org.unbroken-dome.xjc") version "1.4.1"
}

buildscript {
    dependencies {
        classpath("javax.xml.bind:jaxb-api:2.4.0-b180830.0359")
        classpath("org.glassfish.jaxb:jaxb-runtime:2.4.0-b180830.0438")
        classpath("com.sun.activation:javax.activation:1.2.0")
        classpath("com.sun.xml.ws:jaxws-tools:2.3.1") {
            exclude(group = "com.sun.xml.ws", module = "policy")
        }
    }
}

application {
    mainClassName = mainClass
}

repositories {
    maven(url="https://dl.bintray.com/kotlin/ktor")
    maven(url="https://kotlin.bintray.com/kotlinx")
    maven(url="http://packages.confluent.io/maven/")
    mavenCentral()
    jcenter()
}

dependencies {
    compile(kotlin("stdlib"))
    compile("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    compile("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
    compile("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    compile("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
    compile("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    compile("io.ktor:ktor-server-netty:$ktorVersion")
    compile("io.ktor:ktor-auth:$ktorVersion")
    compile("io.ktor:ktor-jackson:$ktorVersion")
    compile("io.ktor:ktor-client-core:$ktorVersion")
    compile("io.ktor:ktor-client-apache:$ktorVersion")
    compile( "io.ktor:ktor-locations:$ktorVersion")
    compile("io.prometheus:simpleclient_common:$prometheusVersion")
    compile("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    compile("io.github.microutils:kotlin-logging:$kotlinloggingVersion")
    compile("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")
    compile("com.natpryce:konfig:$konfigVersion")
    compile("javax.xml.ws:jaxws-api:$jaxwsVersion")

    implementation("org.apache.cxf:cxf-rt-frontend-jaxws:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-features-logging:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-transports-http:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-ws-security:$cxfVersion")
    implementation("org.apache.ws.xmlschema:xmlschema-core:2.2.4") // Force newer version of XMLSchema to fix illegal reflective access warning
    implementation("com.sun.xml.ws:jaxws-tools:$jaxwsToolsVersion") {
        exclude(group = "com.sun.xml.ws", module = "policy")
    }
    
    implementation("javax.activation:activation:$javaxActivationVersion")
    implementation("org.webjars:swagger-ui:$swaggerVersion")
    implementation("com.unboundid:unboundid-ldapsdk:$unboundidVersion")

    runtimeOnly("ch.qos.logback:logback-classic:$logbackVersion")
    runtime("com.papertrailapp:logback-syslog4j:1.0.0")

    testImplementation("org.spekframework.spek2:spek-dsl-jvm:$spekVersion") {
        exclude(group = "org.jetbrains.kotlin")
    }
    testRuntimeOnly ("org.spekframework.spek2:spek-runner-junit5:$spekVersion") {
        exclude(group = "org.jetbrains.kotlin")
    }
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") {
        exclude(group = "org.eclipse.jetty") // conflicts with WireMock
    }
    testImplementation("org.amshove.kluent:kluent:$kluentVersion")
    testImplementation("org.junit.platform:junit-platform-runner:1.3.2")
    testRuntimeOnly("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
}

val generatedSourcesDir = "$buildDir/generated-sources"

tasks {
    create("printVersion") {
        println(project.version)
    }
    withType<KotlinCompile> {
        dependsOn("wsdl2java")
        dependsOn("xjcGenerate")
    }
    withType<Wsdl2JavaTask> {
        wsdlDir = file("$projectDir/src/main/resources/wsdl")
        wsdlsToGenerate = listOf(
            mutableListOf("-xjc", "-b", "$projectDir/src/main/xjb/binding.xml", "$projectDir/src/main/resources/wsdl/RegisterSRRAgencyExternalBasic.wsdl")
        )
        generatedWsdlDir = file(generatedSourcesDir)
    }
    withType<ShadowJar> {
        classifier = ""
        transform(ServiceFileTransformer::class.java) {
            setPath("META-INF/cxf")
            include("bus-extensions.txt")
        }
    }
    withType<Test> {
        useJUnitPlatform {
            includeEngines("spek2")
        }
        testLogging.events("passed", "skipped", "failed")
    }
    withType<Wrapper> {
        gradleVersion = "5.1"
        distributionType = Wrapper.DistributionType.BIN
    }
    xjcGenerate {
        source = fileTree("$projectDir/src/main/resources/xsd") { include("*.xsd") }
        outputDirectory = File(generatedSourcesDir)
    }
}

java {
    sourceSets["main"].java.srcDirs(generatedSourcesDir)
}
