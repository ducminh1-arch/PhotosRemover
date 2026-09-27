# Proguard rules for PhotosRemover
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* *;
}
