import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer
import no.nils.wsdl2java.Wsdl2JavaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "no.nav"
version = "1.0.2-SNAPSHOT"

val kotlinVersion = "1.4.20"
val ktorVersion = "1.4.2"
val kotlinxCoroutinesVersion = "1.4.2"
val jacksonVersion = "2.12.0"

val prometheusVersion = "0.9.0"
val kotlinloggingVersion = "2.0.3"
val logstashEncoderVersion = "6.5"
val logbackVersion = "1.2.3"

val konfigVersion = "1.6.10.0"

val jaxwsVersion = "2.3.1"
val jaxwsToolsVersion = "2.3.1"
val javaxActivationVersion = "1.1.1"
val cxfVersion = "3.3.1"
val unboundidVersion = "5.1.1"
val tjenestespesifikasjonerVersion = "1.2019.09.25-00.21-49b69f0625e0"
val wiremockVersion = "2.27.2"

val swaggerVersion = "3.1.7"

// test dependencies
val kluentVersion = "1.64"
val spekVersion = "2.0.14"
val junitPlatformVersion = "1.7.0"

val appMainClassName = "no.nav.altinn.admin.BootstrapKt"

plugins {
    application
    java
    kotlin("jvm") version "1.4.20"
    id("no.nils.wsdl2java") version "0.10"
    id("org.jmailen.kotlinter") version "3.2.0"
    id("com.github.ben-manes.versions") version "0.36.0"
    id("com.github.johnrengelman.shadow") version "6.1.0"
    id("org.unbroken-dome.xjc") version "2.0.0"
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
    mainClassName = "$appMainClassName"
}
//application {
//    mainClass.set(appMainClassName)
//}

repositories {
    maven(url="https://dl.bintray.com/kotlin/ktor")
    maven(url="https://kotlin.bintray.com/kotlinx")
    maven(url="https://packages.confluent.io/maven/")
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-auth:$ktorVersion")
    implementation("io.ktor:ktor-jackson:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    implementation( "io.ktor:ktor-locations:$ktorVersion")
    implementation("io.prometheus:simpleclient_common:$prometheusVersion")
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    implementation("io.github.microutils:kotlin-logging:$kotlinloggingVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")
    implementation("com.natpryce:konfig:$konfigVersion")
    implementation("javax.xml.ws:jaxws-api:$jaxwsVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:$kotlinxCoroutinesVersion")
    implementation("no.nav.tjenestespesifikasjoner:altinn-download-queue-external:$tjenestespesifikasjonerVersion")
    implementation("no.nav.tjenestespesifikasjoner:altinn-correspondence-agency-external-basic:$tjenestespesifikasjonerVersion")

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
    runtimeOnly("com.papertrailapp:logback-syslog4j:1.0.0")

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
    testImplementation("org.junit.platform:junit-platform-runner:$junitPlatformVersion")
    testRuntimeOnly("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    testImplementation("com.github.tomakehurst:wiremock:$wiremockVersion")
}

val generatedSourcesDir = "$buildDir/generated-sources"

tasks {
    create("printVersion") {
        println(project.version)
    }
    withType<KotlinCompile> {
        dependsOn("wsdl2java")
        // dependsOn("xjcGenerate")
    }
    withType<Wsdl2JavaTask> {
        wsdlDir = file("$projectDir/src/main/resources/wsdl")
        wsdlsToGenerate = listOf(
            mutableListOf("-xjc", "-b", "$projectDir/src/main/xjb/binding.xml", "$projectDir/src/main/resources/wsdl/RegisterSRRAgencyExternalBasic.wsdl"),
            mutableListOf("-xjc", "-b", "$projectDir/src/main/xjb/binding.xml", "$projectDir/src/main/resources/wsdl/ReceiptAgencyExternalBasic.wsdl"),
            mutableListOf("-xjc", "-b", "$projectDir/src/main/xjb/binding.xml", "$projectDir/src/main/resources/wsdl/ServiceOwnerArchiveExternalBasic.wsdl")
        )
        generatedWsdlDir = file(generatedSourcesDir)
    }
    withType<ShadowJar> {
        archiveClassifier.set("")
        transform(ServiceFileTransformer::class.java) {
            setPath("META-INF/cxf")
            include("bus-extensions.txt")
        }
        manifest {
            attributes(
                mapOf(
                    "Main-Class" to appMainClassName
                )
            )
        }
    }
    withType<Test> {
        useJUnitPlatform {
            includeEngines("spek2")
        }
        testLogging.events("passed", "skipped", "failed")
    }
//    withType<Wrapper> {
//        gradleVersion = "6.7.1"
//        distributionType = Wrapper.DistributionType.BIN
//    }
//    xjcGenerate {
//        source = fileTree("$projectDir/src/main/resources/xsd") { include("*.xsd") }
//        outputDirectory = File(generatedSourcesDir)
//    }
}

java {
    sourceSets["main"].java.srcDirs(generatedSourcesDir)
}
