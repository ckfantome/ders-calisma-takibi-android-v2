# Ders Çalışma Takibi

Kamera ile çalışma durumunu (çalışıyor / uzakta / uykulu) gerçek zamanlı tespit eden, ebeveyn-denetimli bir ders çalışma takip uygulaması. Yüz/göz analizi (MediaPipe FaceLandmarker) tamamen cihaz üzerinde çalışır, hiçbir görüntü kaydedilmez veya dışarı gönderilmez.

## Kurulum

[Releases](../../releases) sayfasından telefonuna uygun APK'yı indir:

| Telefon | Dosya |
|---|---|
| Yeni telefonların çoğu | `arm64-v8a` |
| Eski/bazı telefonlar | `armeabi-v7a` |
| Intel tabanlı (nadir) | `x86` |

Emin değilsen `arm64-v8a` dene.

## Neler yapıyor

- **Çalışma tespiti** — ön kameradan yüz/göz/baş pozisyonu analiziyle çalışıyor/uzakta/uykulu durumunu saptar, oturum süresi ve verimlilik yüzdesini kaydeder.
- **Pomodoro + Haftalık Takvim** — çalışma/mola döngüsü ve haftalık ders programı takibi.
- **Yönetici / Öğrenci rolleri** — PIN korumalı yönetici modu, öğrenci modunda ayarların çoğu gizli/salt-okunur.
- **Uygulama Kilidi** — seçilen uygulamaları (günlük süre sınırı veya ders saatlerinde) engeller; **Sınav/Ödev Modu** açıkken izin verilenler dışında her şeyi engeller, isteğe bağlı olarak ekranı tamamen tek uygulamaya kilitler. *Bilinen kısıt:* Bazı telefon üreticilerinin (Xiaomi/MIUI Dual Apps, Samsung Dual Messenger, Work Profile klonları gibi) aynı uygulamayı ikinci bir hesapla çalıştıran "klon uygulama" özelliği, ayrı bir Android kullanıcı kimliği altında çalıştığı için Erişilebilirlik Servisi'ne bu klonun ekran olayları hiç ulaşmaz — bu yüzden platform seviyesinde engellenemez. Paket adı farklı olan üçüncü parti klon uygulamaları (Parallel Space, App Cloner vb.) ise normal bir uygulama gibi listeye eklenip engellenebilir.
- **Güvenli Bölge** — birden fazla konum tanımlanabilir, cihaz bölge dışına çıkınca anlık uyarı gönderir.
- **Arama/SMS özeti, bildirim logu, klavye takibi** — hepsi ayrı izinlerle korunur, varsayılan olarak kapalı olanlar (klavye takibi gibi) açıkça etkinleştirilmeden çalışmaz; şifre alanları hiçbir zaman kaydedilmez.
- **Günlük yedek e-postası** — toplanan verileri (hangilerinin dahil edileceği seçilebilir) her gün otomatik olarak belirlenen bir e-posta adresine gönderir.
- **Kalıcılık** — cihaz yeniden başlayınca veya uygulama kapatılmaya çalışılınca arkaplan takibini otomatik yeniden başlatma, Cihaz Yöneticisi ile kaldırma koruması.
- **Uygulama içi güncelleme** — GitHub Releases üzerinden yeni sürüm kontrolü ve kurulumu.

## Gizlilik

Tüm veriler varsayılan olarak yalnızca cihazda saklanır. Kamera görüntüsü hiçbir zaman kaydedilmez — yalnızca anlık analiz sonucu (durum) tutulur. Uygulama içindeki Gizlilik Politikası ekranında tam detay var.

## Teknik

Kotlin + Jetpack Compose, CameraX + MediaPipe Tasks Vision, Room + DataStore, WorkManager. minSdk 24 (Android 7.0+).
