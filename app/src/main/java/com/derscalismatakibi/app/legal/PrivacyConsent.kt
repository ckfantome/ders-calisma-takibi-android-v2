package com.derscalismatakibi.app.legal

/** KVKK/gizlilik aydinlatma metni. VERSION artirilirsa AppNavigation kullaniciyi
 * (privacyConsentVersion < VERSION oldugu icin) yeniden onay ekranina yonlendirir. */
object PrivacyConsent {
    const val VERSION = 4

    val TEXT = """
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
        - KLAVYE TAKİBİ (yalnızca Uygulama Kilidi ekranından AYRICA ve AÇIKÇA etkinleştirilirse - varsayılan KAPALIDIR): Erişilebilirlik Servisi, diğer uygulamalarda YAZILAN METİNLERİ de kaydeder. ŞİFRE ALANLARI (parola girişleri) bu kayıttan HER ZAMAN hariç tutulur. Bu özellik açıkken Erişilebilirlik Servisi'nin "ekran içeriğini okumaz" varsayılan davranışı GEÇERSİZ olur.

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
}
