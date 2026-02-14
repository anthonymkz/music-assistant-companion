# Compose
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class io.music_assistant.client.**$$serializer { *; }
-keepclassmembers class io.music_assistant.client.** {
    *** Companion;
}
-keepclasseswithmembers class io.music_assistant.client.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor
-dontwarn io.ktor.**
-keep class io.ktor.** { *; }

# Koin
-keep class org.koin.** { *; }

# Coil
-dontwarn coil3.**
-keep class coil3.** { *; }

# Keep data model classes (used by serialization/reflection)
-keep class io.music_assistant.client.data.model.** { *; }

# SLF4J (used by Ktor)
-dontwarn org.slf4j.**
