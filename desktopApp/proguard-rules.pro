# 1. 忽略 Skiko 和 JetBrains Runtime (JBR) 特有类的缺失警告
-dontwarn androidx.compose.ui.**
-dontwarn com.jetbrains.**
-dontwarn org.jetbrains.skiko.**
-dontwarn org.jetbrains.skia.**

-keep class androidx.compose.ui.platform.** { *; }

# 2. 忽略常用的第三方库缺失依赖警告
-dontwarn com.sun.jna.**
-dontwarn org.freedesktop.dbus.**
-dontwarn org.slf4j.**

# 3. 保留 Skiko 渲染引擎核心类不被裁切
-keep class org.jetbrains.skiko.** { *; }
-keep class com.jetbrains.** { *; }

# 4. 保留 JNA / FFmpeg 等 Native C/C++ 调用的原生方法
-keepclasseswithmembernames class * {
    native <methods>;
}


# 完整保留 JNA 及其所有子包、内部类、属性与方法
-keep class com.sun.jna.** { *; }
-keepclassmembers class com.sun.jna.** { *; }

# 保留实现 JNA 接口的类及内部类
-keep class * implements com.sun.jna.** { *; }

-dontwarn com.sun.jna.**

# 保留与 Native 交互的接口和实现
-keep interface com.sun.jna.** { *; }
-keepclassmembers class * implements com.sun.jna.** { *; }

-keepattributes InnerClasses, Signature
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepattributes StackMapTable, StackMap, LineNumberTable

-dontoptimize

-keep class androidx.compose.runtime.** { *; }

-keepattributes RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations, RuntimeVisibleParameterAnnotations, RuntimeInvisibleParameterAnnotations