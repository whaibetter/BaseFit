# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the Android SDK tools.
# For more details, see
#   https://developer.android.com/build/shrink-code

# Keep Room entities
-keep class com.basefit.app.data.entity.** { *; }

# Keep data classes
-keep class **$$serializer { *; }
-keepclassmembers class * {
    *** companion object;
}
