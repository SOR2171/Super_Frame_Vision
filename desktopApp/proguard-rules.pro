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

# 5. 保留 Kotlin 序列化与反射注解
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod