# MediaPipe'in kullandigi Google Flogger, CAGRI YIGININI (stack) yururek
# "gercek cagiran" sinifi buluyor - R8'in metod inline/birlestirme
# optimizasyonu bu yigin sekli degistirince "IllegalStateException: no caller
# found on the stack for: com.google.common.flogger.FluentLogger" ile
# coküyordu (gercek emulator testinde dogrulandi, -dontobfuscate TEK BASINA
# yetmedi). Kod kucultme (kullanilmayan sinif/kaynak silme) HALA calisir,
# sadece TUTULAN kodun METODLARI yeniden yapilandirilmiyor/inline edilmiyor.
-dontobfuscate
-dontoptimize

# MediaPipe Tasks API agir reflection/protobuf kullanir - agresif kucultme
# calisma zamaninda ClassNotFoundException'a yol acar.
-keep class com.google.mediapipe.** { *; }
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.mediapipe.**
-dontwarn com.google.protobuf.**

# OpenCV JNI native metod baglamalari - isim/imza degisirse UnsatisfiedLinkError.
-keep class org.opencv.** { *; }
-dontwarn org.opencv.**

# Room (runtime annotation + generated DAO/Database siniflari).
-keep class androidx.room.** { *; }
-keepattributes *Annotation*
-keepattributes Signature

# JavaMail/Activation Provider'lari META-INF/javamail.providers uzerinden
# reflection ile yukler - sinif adlari degismemeli.
-keep class com.sun.mail.** { *; }
-keep class javax.mail.** { *; }
-keep class javax.activation.** { *; }
-dontwarn com.sun.mail.**
-dontwarn javax.mail.**
-dontwarn javax.activation.**

# MediaPipe/protobuf'un transitif bagimliliklari (AutoValue) derleme-zamani
# annotation processor siniflarina (JDK'nin javax.annotation.processing/
# javax.lang.model paketleri) referans birakiyor - bunlar calisma zamaninda
# hic cagrilmaz, Android'de zaten yok, R8 "missing class" hatasi vermesin diye.
-dontwarn javax.annotation.processing.**
-dontwarn javax.lang.model.**
-dontwarn javax.tools.**
-dontwarn autovalue.shaded.**
-dontwarn com.google.auto.value.**

# Uygulamanin kendi veri siniflari (Room entity/DataStore AppConfig) - alan
# adlari serilestirme/sorgu eslemesinde kullaniliyor.
-keep class com.derscalismatakibi.app.data.** { *; }
-keep class com.derscalismatakibi.app.core.AppConfig { *; }
