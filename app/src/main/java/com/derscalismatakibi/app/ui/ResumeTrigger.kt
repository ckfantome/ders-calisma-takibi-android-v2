package com.derscalismatakibi.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * Ozel-erisim izinleri (Erisilebilirlik, Kullanim Erisimi, Bildirim Dinleyici vb.)
 * ayri bir Ayarlar ekranindan verilir - kullanici izni verip GERI DONDUGUNDE
 * ayni Activity/composable hala hayatta oldugu icin (bkz. AppNavigation'daki
 * popUpTo(saveState=true)/restoreState=true), 'remember'lanmis izin durumu asla
 * tazelenmiyordu; kullaniciya "izin verildi ama uygulama hala gormuyor" gibi
 * gorunuyordu. Bu, o ekranlarin LaunchedEffect anahtarina eklenip her ON_RESUME'da
 * izin kontrolunu yeniden calistirmayi saglar.
 */
@Composable
fun rememberResumeTrigger(): Int {
    var trigger by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) trigger++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return trigger
}
