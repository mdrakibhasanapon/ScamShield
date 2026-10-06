# ProGuard rules for ScamShield
-keepclassmembers class * {
    @androidx.annotation.Keep <fields>;
    @androidx.annotation.Keep <methods>;
}

-keep class com.scamshield.app.data.model.** { *; }
-dontwarn java.lang.invoke.**
