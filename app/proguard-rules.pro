# ─────────────────────────────────────────────────────────────────
# Floating Screen Utility — ProGuard Rules
# ─────────────────────────────────────────────────────────────────

# Keep the application class
-keep class com.floatingscreen.FloatingScreenApp { *; }

# ─── Hilt / Dagger ───────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keepclasseswithmembernames class * {
    @dagger.hilt.* <methods>;
}
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }

# ─── Room ────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# ─── Kotlin Coroutines ───────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ─── Kotlin serialization ────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# ─── Parcelize ───────────────────────────────────────────────────
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

# ─── Domain models (used in Intents/Bundles) ─────────────────────
-keep class com.floatingscreen.domain.model.** { *; }

# ─── Android Services / Receivers ───────────────────────────────
-keep class com.floatingscreen.service.** { *; }
-keep class com.floatingscreen.utils.BootReceiver { *; }

# ─── Coil ────────────────────────────────────────────────────────
-keep class coil.** { *; }

# ─── Timber ──────────────────────────────────────────────────────
-keep class timber.log.** { *; }

# ─── Gson ────────────────────────────────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ─── General ─────────────────────────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes Exceptions
-dontwarn org.jetbrains.annotations.**
