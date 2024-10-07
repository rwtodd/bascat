plugins {
    java
    application
}

version = "1.0.0"
group = "org.rwtodd"

repositories {
}

dependencies {
}

tasks.withType<JavaCompile>().configureEach {
   options.release = 21
}

application {
    applicationName = "bascat"
    mainModule = "org.rwtodd.bascat"
    mainClass = "org.rwtodd.bascat.Cmd"
}
