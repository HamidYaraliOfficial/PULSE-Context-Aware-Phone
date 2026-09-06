# PULSE ProGuard rules
-keep class com.pulse.app.database.entities.** { *; }
-keep class com.pulse.app.domain.model.** { *; }
-keepattributes *Annotation*
-keepclassmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel *;
}
-dontwarn kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
