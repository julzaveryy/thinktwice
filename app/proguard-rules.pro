# kotlinx.serialization: keep generated serializers for content DTOs and navigation routes.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class com.miqu.thinktwice.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.miqu.thinktwice.** {
    kotlinx.serialization.KSerializer serializer(...);
}
