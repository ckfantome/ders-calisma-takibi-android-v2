package com.derscalismatakibi.app.legal

import android.content.Context

/** KVKK/gizlilik aydinlatma metni. VERSION artirilirsa AppNavigation kullaniciyi
 * (privacyConsentVersion < VERSION oldugu icin) yeniden onay ekranina yonlendirir.
 * Metin uzun/bicimli oldugu icin XML string resource yerine dile gore secilen iki
 * sabit olarak tutuluyor (bkz. text(context)). */
object PrivacyConsent {
    const val VERSION = 5

    fun text(context: Context): String =
        if (context.resources.configuration.locales[0].language == "en") TEXT_EN else TEXT_TR

    val TEXT_TR = """
        GİZLİLİK VE KİŞİSEL VERİLERİN İŞLENMESİ HAKKINDA BİLGİLENDİRME

        Bu metin, "Ders Çalışma Takibi" uygulamasının hangi verileri topladığını, nasıl kullandığını ve saklama şeklini açıklar. Bir hukuki danışmanlık metni değildir; genel bilgilendirme amaçlıdır.

        1. VERİ SORUMLUSU
        Bu uygulamayı kuran ve yöneten kişi "Uygulama Yöneticisi (Ebeveyn/Veli)" sıfatıyla, uygulama içindeki Yönetici PIN'i ile korunan ayarları kontrol eder ve toplanan verilere erişebilir.

        2. TOPLANAN VERİLER (ŞU AN AKTİF ÖZELLİKLER)
        - Kamera görüntüsü: Kamera görüntüsü CİHAZDA ANLIK işlenir, KAYDEDİLMEZ VE SAKLANMAZ. Sadece yüz/göz/baş pozisyonu analiziyle türetilen "çalışıyor / uzakta / uykulu" durumu kaydedilir.
        - Çalışma oturumu verileri: başlangıç/bitiş saatleri, çalışma-uzakta-uyku süreleri, verimlilik yüzdesi, eklenen notlar ve etiketler.
        - Pomodoro ve haftalık ders programı (takvim) verileri.
        - Diğer uygulamalarda geçirilen süre: yalnızca "Kullanım Erişimi" izni ayrıca ve açıkça verilirse toplanır; uygulama adı ve süre bilgisi içerir.
        - Teşhis/hata kayıtları (Loglar): uygulamanın düzgün çalışıp çalışmadığını anlamak için servis olayları, hata mesajları kaydedilir.
        - Arama/mesaj özeti: yalnızca ilgili izin (Arama Geçmişi/SMS) ayrıca ve açıkça verilirse, son aramalar (numara/ad, süre, tarih) ve son SMS'ler (gönderen, kısa önizleme, tarih) görüntülenir.
        - Bildirim ve uygulama açılış kaydı: yalnızca "Bildirim Erişimi" izni ayrıca verilirse diğer uygulamalardan gelen bildirimlerin başlığı/metni; "Kullanım Erişimi" izniyle hangi uygulamanın ne zaman açılıp kapandığı kaydedilir.
        - Konum ve güvenli bölge: yalnızca Konum izni ayrıca verilirse ve "Güvenli Bölge" özelliği açılırsa, cihazın güvenli bölgeye göre yaklaşık konumu ve bölgeye giriş/çıkış zamanları kaydedilir.
        - Uygulama engelleme/ön plan izleme: yalnızca "Erişilebilirlik Servisi" izni ayrıca verilirse ve Uygulama Kilidi listesine bir uygulama eklenirse, hangi uygulamanın ön planda olduğu tespit edilir; engellenmiş bir uygulama açılırsa kapatılıp nedeni gösterilir. Sınav/Ödev Modu açıkken, izin verilenler listesi dışındaki TÜM uygulamalar (Kilitli Uygulamalar listesine bakılmaksızın) engellenir.
        - KLAVYE TAKİBİ (yalnızca Uygulama Kilidi ekranından AYRICA ve AÇIKÇA etkinleştirilirse - varsayılan KAPALIDIR): Erişilebilirlik Servisi, diğer uygulamalarda YAZILAN METİNLERİ de kaydeder. Bu kayıt, uygulamaların kullandığı arayüz teknolojisinden bağımsız olarak (klasik metin kutuları dahil, modern arayüz bileşenleri ve uygulama-içi web sayfaları/tarayıcı alanları dahil) daha geniş bir kapsamda çalışır. ŞİFRE ALANLARI (parola girişleri) bu kayıttan HER ZAMAN hariç tutulur; sistem şifre alanı olup olmadığını güvenilir şekilde belirleyemediği durumlarda da, güvenlik gereği metin KAYDEDİLMEZ. Bu özellik açıkken Erişilebilirlik Servisi'nin "ekran içeriğini okumaz" varsayılan davranışı GEÇERSİZ olur.

        3. VERİLERİN SAKLANMASI
        Yukarıdaki tüm veriler ŞU AN SADECE bu cihazın kendi deposunda (yerel veritabanı ve dosyalar) tutulur; bir sunucuya otomatik gönderilmez. İSTEĞE BAĞLI "Günlük Otomatik Yedekleme" özelliği açılırsa, veriler Yönetici'nin Ayarlar'dan kendi belirlediği bir e-posta adresine gönderilir - bu özellik varsayılan olarak KAPALIDIR ve yalnızca Yönetici tarafından açılabilir.

        4. GELECEKTEKİ DEĞİŞİKLİKLER
        Uygulamaya ileride bulut tabanlı uzaktan izleme, ek cihazlarla senkronizasyon veya üçüncü kişilerle (örn. öğretmen) paylaşım gibi özellikler eklenirse, veriler bu cihazın dışına da çıkabilir hale gelir. Böyle bir değişiklik öncesinde bu metin güncellenecek ve yeniden onayınız istenecektir.

        5. AMAÇ
        Veriler yalnızca ders çalışma alışkanlığının takibi, verimliliğin artırılması ve uygulamanın düzgün çalıştığının denetlenmesi amacıyla işlenir.

        6. HAKLARINIZ
        Bu metni istediğiniz zaman Ayarlar bölümünden tekrar okuyabilirsiniz. Kayıtlı verilerinizi "Dışa Aktar" özelliğiyle görebilir, Uygulama Yöneticisi'nden verilerin silinmesini talep edebilirsiniz.

        7. ONAY
        "Okudum, anladım, kabul ediyorum" kutusunu işaretleyip devam ederek bu bilgilendirmeyi okuduğunuzu ve verilerin yukarıda açıklanan şekilde işlenmesini kabul ettiğinizi beyan edersiniz.
    """.trimIndent()

    val TEXT_EN = """
        NOTICE ON PRIVACY AND PROCESSING OF PERSONAL DATA

        This text explains what data the "Ders Çalışma Takibi" (Study Tracker) app collects, how it uses it, and how it is stored. It is not legal advice; it is for general information purposes only.

        1. DATA CONTROLLER
        The person who installs and manages this app acts as the "App Administrator (Parent/Guardian)", controls the settings protected by the in-app Administrator PIN, and can access the collected data.

        2. DATA COLLECTED (CURRENTLY ACTIVE FEATURES)
        - Camera image: The camera image is processed INSTANTLY ON THE DEVICE, NEVER RECORDED OR STORED. Only the "studying / away / drowsy" state derived from face/eye/head position analysis is saved.
        - Study session data: start/end times, study-away-sleep durations, productivity percentage, added notes and tags.
        - Pomodoro and weekly class schedule (calendar) data.
        - Time spent in other apps: collected only if "Usage Access" permission is separately and explicitly granted; includes app name and duration.
        - Diagnostic/error logs (Logs): service events and error messages are recorded to understand whether the app is working properly.
        - Call/message summary: only if the relevant permission (Call History/SMS) is separately and explicitly granted, recent calls (number/name, duration, date) and recent SMS messages (sender, short preview, date) are shown.
        - Notification and app-launch log: only if "Notification Access" permission is separately granted, the title/text of notifications from other apps; with "Usage Access" permission, which app opened/closed and when.
        - Location and safe zone: only if Location permission is separately granted and the "Safe Zone" feature is turned on, the device's approximate location relative to the safe zone and entry/exit times are recorded.
        - App blocking/foreground monitoring: only if "Accessibility Service" permission is separately granted and an app is added to the App Lock list, which app is in the foreground is detected; if a blocked app is opened it is closed and the reason shown. While Exam/Homework Mode is on, ALL apps outside the allowed list (regardless of the Locked Apps list) are blocked.
        - KEYSTROKE LOGGING (only if SEPARATELY and EXPLICITLY enabled from the App Lock screen - OFF by default): the Accessibility Service also records TEXT TYPED in other apps. This logging works across a broader range regardless of the interface technology an app uses (classic text fields, as well as modern UI components and in-app web pages/browser fields). PASSWORD FIELDS are ALWAYS excluded from this logging; if the system cannot reliably determine whether a field is a password field, the text is likewise NOT RECORDED, for safety. While this feature is on, the Accessibility Service's default "does not read screen content" behavior no longer applies.

        3. DATA STORAGE
        All the data above is CURRENTLY stored ONLY in this device's own storage (local database and files); it is not automatically sent to a server. If the OPTIONAL "Daily Automatic Backup" feature is turned on, data is sent to an email address the Administrator sets in Settings - this feature is OFF by default and can only be turned on by the Administrator.

        4. FUTURE CHANGES
        If cloud-based remote monitoring, syncing with additional devices, or sharing with third parties (e.g. a teacher) is added to the app in the future, data may leave this device as well. This text will be updated and your consent requested again before any such change.

        5. PURPOSE
        Data is processed solely to track study habits, improve productivity, and verify that the app is working correctly.

        6. YOUR RIGHTS
        You can re-read this text at any time from the Settings section. You can view your recorded data using the "Export" feature, and request the App Administrator to delete your data.

        7. CONSENT
        By checking the "I have read, understood, and agree" box and continuing, you declare that you have read this notice and agree to the processing of data as described above.
    """.trimIndent()
}
