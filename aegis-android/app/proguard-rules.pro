# Add project specific ProGuard rules here.

# ── Debug info (keep for crash reporting) ─────────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Kotlin ─────────────────────────────────────────────────────────────────────
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }
-keep class kotlin.coroutines.** { *; }
-dontwarn kotlin.**

# ── Hilt / Dagger ──────────────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keep @javax.inject.Singleton class * { *; }
-keepclasseswithmembernames class * { @javax.inject.Inject <fields>; }
-keepclasseswithmembernames class * { @javax.inject.Inject <init>(...); }

# ── Room ───────────────────────────────────────────────────────────────────────
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Database class * { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { abstract *; }

# ── Kotlinx Serialization ──────────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.aegis.app.**$$serializer { *; }
-keepclassmembers class com.aegis.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.aegis.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Supabase / Ktor ────────────────────────────────────────────────────────────
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# ── Retrofit / OkHttp ─────────────────────────────────────────────────────────
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-dontwarn retrofit2.**
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# ── Firebase / FCM ─────────────────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ── EncryptedSharedPreferences / Security Crypto ──────────────────────────────
-keep class androidx.security.crypto.** { *; }

# ── Data models (Retrofit/Gson serialization) ─────────────────────────────────
-keep class com.aegis.app.data.model.** { *; }
-keep class com.aegis.app.data.local.entity.** { *; }