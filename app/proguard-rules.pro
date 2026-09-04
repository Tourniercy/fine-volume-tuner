# Keep loudness effect classes (reflected by audio framework)
-keep class android.media.audiofx.** { *; }
-keepclassmembers class app.rilcy.finevolumetuner.** {
    *** Companion;
}
# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class app.rilcy.finevolumetuner.** {
    *** Companion;
}
-keepclasseswithmembers class app.rilcy.finevolumetuner.** {
    kotlinx.serialization.KSerializer serializer(...);
}
