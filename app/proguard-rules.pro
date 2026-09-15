# Proguard rules for PacePilot
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
-dontwarn org.osmdroid.**
