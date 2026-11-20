# Godot finds the plugin by the class name in the manifest and calls its methods by name.
-keep class studio.chaosmos.intermission.IntermissionPlugin { *; }
# The plugin creates the provider named in the provider library's manifest by reflection.
-keep class * implements studio.chaosmos.intermission.Provider {
    public <init>();
}
