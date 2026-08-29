package com.derscalismatakibi.app

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.derscalismatakibi.app.R
import com.derscalismatakibi.app.core.BlockReason
import com.derscalismatakibi.app.data.SettingsRepository
import com.derscalismatakibi.app.ui.theme.DersCalismaTakibiTheme
import java.util.Locale

/** AppBlockAccessibilityService bir engelli uygulama tespit edince bunu on plana
 * getirir - kullaniciya (cocuga) NEDEN engellendigini acikca gosterir, sessizce
 * kapatmaz. study_tracker2.py'de karsiligi yok, Android'e ozgu. */
class BlockedActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val language = SettingsRepository.LocalePrefs.read(newBase)
        val locale = Locale(language)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val reasonName = intent.getStringExtra(EXTRA_REASON)
        setContent {
            DersCalismaTakibiTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val label = when (reasonName) {
                        "ExamMode" -> stringResource(R.string.blocked_reason_exam_mode)
                        "DailyLimit" -> stringResource(R.string.blocked_reason_daily_limit)
                        else -> stringResource(R.string.blocked_reason_study_hours)
                    }
                    BlockedScreen(label) {
                        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
                        finish()
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_REASON = "reason"

        fun reasonExtra(reason: BlockReason): String = when (reason) {
            is BlockReason.ExamMode -> "ExamMode"
            is BlockReason.DailyLimit -> "DailyLimit"
            is BlockReason.StudyHours -> "StudyHours"
        }
    }
}

@Composable
private fun BlockedScreen(reasonLabel: String, onHome: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.blocked_title), style = MaterialTheme.typography.headlineSmall)
        Text(reasonLabel, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 12.dp, bottom = 24.dp))
        Button(onClick = onHome) { Text(stringResource(R.string.blocked_go_home)) }
    }
}
