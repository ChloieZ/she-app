# ── 高德地图 SDK ──
-keep class com.amap.api.** { *; }
-keep class com.autonavi.** { *; }
-keep class com.amap.api.maps.** { *; }
-dontwarn com.amap.api.**
-dontwarn com.autonavi.**

# ── 泛型签名（Gson/Retrofit 反序列化必需）──
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, RuntimeInvisibleAnnotations
-keepattributes AnnotationDefault
-keepattributes *Annotation*
-keepattributes GenericSignature

# ── Retrofit 接口（保留全部方法签名和泛型返回类型）──
-keep,allowobfuscation,allowshrinking interface com.herspace.app.data.api.HerSpaceApi
-keepclassmembers interface com.herspace.app.data.api.HerSpaceApi {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# ── Gson（保留泛型类型信息）──
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ── App 数据模型（不混淆，Gson 反序列化需要保持类名和字段名）──
-keep class com.herspace.app.data.api.** { *; }
-keep class com.herspace.app.data.model.** { *; }
-keep class com.herspace.app.data.db.** { *; }
-keep class com.herspace.app.util.SearchResult { *; }

# ── App ViewModel/UI 层（保持不被删）──
-keep class com.herspace.app.ui.vote.VoteViewModel { *; }
-keep class com.herspace.app.ui.map.MapViewModel { *; }
-keep class com.herspace.app.ui.auth.AuthViewModel { *; }

# ── Room ──
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ── Kotlin Coroutines ──
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ── Kotlin Metadata（Retrofit suspend 函数返回类型解析必需）──
-keep @kotlin.Metadata class * { *; }
-keep class kotlin.Metadata { *; }

# ── Compose ──
-keep class androidx.compose.** { *; }

# ── Enum ──
-keepclassmembers enum * { *; }
