-dontwarn androidx.compose.ui.**
-dontwarn com.jetbrains.**
-dontwarn org.jetbrains.skiko.**
-dontwarn org.jetbrains.skia.**

-dontwarn com.sun.jna.**
-dontwarn org.freedesktop.dbus.**
-dontwarn org.slf4j.**

-keep class androidx.compose.ui.platform.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class org.jetbrains.skiko.** { *; }
-keep class com.jetbrains.** { *; }
-keep interface com.sun.jna.** { *; }
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }
-keep class io.github.sor2171.superframevision.core.utils.VulkanDeviceDetector** { *; }
-keep class io.github.sor2171.superframevision.core.service.NcnnRunner** { *; }
-keep class io.github.sor2171.superframevision.core.service.NcnnLibrary** { *; }
-keep class org.slf4j.** { *; }
-keepclassmembers class org.slf4j.** { *; }
-keepclassmembers class com.sun.jna.** { *; }
-keepclassmembers class * implements com.sun.jna.** { *; }
-keepclassmembers class * extends com.sun.jna.** {
    <fields>;
    public <init>();
}
-keepclasseswithmembernames class * {
    native <methods>;
}

-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepattributes StackMapTable, StackMap, LineNumberTable
-keepattributes RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations, RuntimeVisibleParameterAnnotations, RuntimeInvisibleParameterAnnotations

-ignorewarnings
-dontoptimize
-dontshrink
-dontobfuscate

-printmapping build/compose/logs/proguardReleaseJars/mapping.txt
-printseeds build/compose/logs/proguardReleaseJars/seeds.txt