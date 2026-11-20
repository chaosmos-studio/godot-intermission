plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
}

val version: String = libs.versions.intermission.get()
val addonDir = layout.projectDirectory.dir("demo/addons/intermission")

// Godot reads the libraries from the addon, so a build copies them there under stable names.
val addonBinaries by tasks.registering(Copy::class) {
    group = "intermission"
    description = "Builds both libraries and copies them into the addon."
    dependsOn(":plugin:assemble", ":levelplay:assemble")
    into(addonDir.dir("bin"))
    for (variant in listOf("debug", "release")) {
        from(project(":plugin").layout.buildDirectory.file("outputs/aar/plugin-$variant.aar")) {
            into(variant)
            rename { "intermission-$variant.aar" }
        }
        from(project(":levelplay").layout.buildDirectory.file("outputs/aar/levelplay-$variant.aar")) {
            into(variant)
            rename { "intermission-levelplay-$variant.aar" }
        }
    }
}

val checkAddonVersion by tasks.registering {
    group = "intermission"
    description = "Fails when plugin.cfg and the version catalog name different versions."
    val cfg = addonDir.file("plugin.cfg").asFile
    doLast {
        val found = Regex("""(?m)^version="([^"]+)"""").find(cfg.readText())?.groupValues?.get(1)
        check(found == version) { "plugin.cfg says $found, the version catalog says $version" }
    }
}

val addonZip by tasks.registering(Zip::class) {
    group = "intermission"
    description = "Packs the addon, with its libraries, as a release download."
    dependsOn(addonBinaries, checkAddonVersion)
    archiveFileName.set("intermission-$version.zip")
    destinationDirectory.set(layout.buildDirectory.dir("dist"))
    from(addonDir) {
        into("addons/intermission")
        exclude("**/*.import")
    }
    from(layout.projectDirectory.file("LICENSE")) { into("addons/intermission") }
}
