plugins {
    kotlin("jvm") version "1.6.21" apply false
    id("cc.polyfrost.multi-version.root")
    id("com.github.johnrengelman.shadow") version "7.1.2" apply false
    id("io.github.juuxel.loom-quiltflower-mini") version "171a6e2e49" apply false
}

preprocess {
    "1.12.2-forge"(11202, "srg") {}
}