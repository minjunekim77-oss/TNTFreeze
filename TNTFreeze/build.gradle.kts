plugins { java }

group = "dev.tntfreeze"
version = "0.2.0-experimental"

repositories { maven("https://repo.papermc.io/repository/maven-public/") }

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.3.build.214-stable")
}

java { toolchain.languageVersion.set(JavaLanguageVersion.of(25)) }
tasks.withType<JavaCompile> { options.encoding = "UTF-8" }
