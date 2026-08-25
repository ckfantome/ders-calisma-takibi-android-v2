package com.derscalismatakibi.app.legal

/** KVKK/gizlilik aydinlatma metni. VERSION artirilirsa AppNavigation kullaniciyi
 * (privacyConsentVersion < VERSION oldugu icin) yeniden onay ekranina yonlendirir. */
object PrivacyConsent {
    const val VERSION = 1

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
