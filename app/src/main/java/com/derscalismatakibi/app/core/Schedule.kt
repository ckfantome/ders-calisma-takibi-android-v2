package com.derscalismatakibi.app.core

import android.content.Context
import com.derscalismatakibi.app.R
import java.util.Calendar

/** study_tracker2.py -> SLOT_KIND_WORK / SLOT_KIND_BREAK / SLOT_KIND_LABELS.
 * ONEMLI: SLOT_KIND_WORK/BREAK degerleri veritabaninda (ScheduleSlotEntity.kind)
 * saklaniyor - DIL DEGISTIRSE BILE bunlar SABIT kalmali, sadece goruntulenen
 * etiket (slotKindLabels) dile gore degisir. */
const val SLOT_KIND_WORK = "calisma"
const val SLOT_KIND_BREAK = "mola"
fun slotKindLabels(context: Context): Map<String, String> = mapOf(
    SLOT_KIND_WORK to context.getString(R.string.schedule_kind_work),
    SLOT_KIND_BREAK to context.getString(R.string.schedule_kind_break),
)

fun weekdayNames(context: Context): List<String> = listOf(
    context.getString(R.string.weekday_monday),
    context.getString(R.string.weekday_tuesday),
    context.getString(R.string.weekday_wednesday),
    context.getString(R.string.weekday_thursday),
    context.getString(R.string.weekday_friday),
    context.getString(R.string.weekday_saturday),
    context.getString(R.string.weekday_sunday),
)

/** study_tracker2.py -> Session.start_time.weekday() (Pazartesi=0..Pazar=6) ile ayni endeksleme. */
fun mondayFirstWeekday(cal: Calendar = Calendar.getInstance()): Int {
    // Calendar.DAY_OF_WEEK: Pazar=1..Cumartesi=7 -> Pazartesi=0..Pazar=6'ya cevir.
    val javaDow = cal.get(Calendar.DAY_OF_WEEK) // 1=Sunday..7=Saturday
    return (javaDow + 5) % 7
}

data class ScheduleSlot(
    val day: Int, // 0=Pazartesi .. 6=Pazar
    val startTime: String, // "HH:mm"
    val endTime: String, // "HH:mm"
    val kind: String, // SLOT_KIND_WORK / SLOT_KIND_BREAK
)

/** study_tracker2.py -> slot_duration_minutes(): gece yarisini gecen araliklar dahil. */
fun slotDurationMinutes(startTime: String, endTime: String): Int {
    val start = parseHm(startTime) ?: return 0
    val end = parseHm(endTime) ?: return 0
    var endMin = end
    if (endMin < start) endMin += 24 * 60
    return endMin - start
}

/** "HH:mm" -> gunun basindan itibaren gecen dakika, format hatali ise null. */
fun parseHm(value: String): Int? {
    val parts = value.split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    return h * 60 + m
}

/**
 * study_tracker2.py -> current_schedule_slot(): su an planlanmis bir aralikta
 * ise o slotu, degilse null dondurur.
 */
fun currentScheduleSlot(slots: List<ScheduleSlot>, day: Int, nowMinuteOfDay: Int): ScheduleSlot? {
    for (slot in slots) {
        if (slot.day != day) continue
        val start = parseHm(slot.startTime) ?: continue
        val end = parseHm(slot.endTime) ?: continue
        if (start <= end) {
            if (nowMinuteOfDay in start..end) return slot
        } else {
            // Gece yarisini gecen aralik (orn. 22:00-01:00).
            if (nowMinuteOfDay >= start || nowMinuteOfDay <= end) return slot
        }
    }
    return null
}
