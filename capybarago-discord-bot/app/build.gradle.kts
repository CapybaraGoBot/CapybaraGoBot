plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://m2.dv8tion.net/releases") 
    maven("https://jitpack.io") 
}

dependencies {
    implementation(libs.guava)
    
    // Discord
    implementation("net.dv8tion:JDA:5.0.0-beta.12")
    implementation("com.github.minndevelopment:jda-ktx:0.10.0-beta.1")

    // Logback 
    implementation("ch.qos.logback:logback-classic:1.2.11")

    // JUnit Jupiter 
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.1")

   // DataBase 

   implementation("org.mariadb.jdbc:mariadb-java-client:3.0.8")
   
   // Env
   
   implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter("5.11.1")
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    mainClass.set("net.jre.AppKt")
}
