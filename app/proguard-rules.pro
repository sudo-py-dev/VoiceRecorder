# ---------------------------------------------------------------------------------
# Aggressive Shrinking & Optimization Rules for < 1MB Target
# ---------------------------------------------------------------------------------

# Use R8 full mode (assumes android.enableR8.fullMode=true in gradle.properties)

# Optimization passes
-optimizationpasses 5
-overloadaggressively
-allowaccessmodification
-mergeinterfacesaggressively

# Repackaging for maximum obfuscation and size reduction
-repackageclasses 'a'

# Remove all logging
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# Remove Kotlin metadata (saves a lot of space)
-dontwarn kotlin.Metadata
-keepclassmembers class ** {
    @kotlin.jvm.JvmField *;
}

# Keep Compose metadata minimally
-keepclassmembers class * {
    @androidx.compose.runtime.Composable class *;
}

# Room Database Optimization
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
# Keep only necessary parts of entities
-keepclassmembers class com.voicerecorder.data.local.** {
    <fields>;
    <init>(...);
}
# Keep DAOs
-keep interface com.voicerecorder.data.local.*Dao { *; }

# Domain models - remove keep of whole package, only keep fields for reflection if needed
-keepclassmembers class com.voicerecorder.domain.model.** {
    <fields>;
    <init>(...);
}

# Coroutines - strip internal names
-keepnames class kotlinx.coroutines.internal.DiagnosticCoroutineContextException
-dontwarn kotlinx.coroutines.**

# Strip source files and line numbers
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Specific to this app: remove debug and tool previews
-dontwarn androidx.compose.ui.tooling.**
-dontwarn androidx.compose.ui.test.**

# Optimization: Inline small methods
-optimizations !code/allocation/variable

# Remove all unnecessary attributes
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
