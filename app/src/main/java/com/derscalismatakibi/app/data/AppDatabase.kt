package com.derscalismatakibi.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 -> v2: `schedule_slots` tablosu eklendi (Takvim Takip Modu). Bu SQL,
 * ScheduleSlotEntity'nin (day INTEGER, start_time/end_time/kind TEXT,
 * autoGenerate Long id) Room'un urettigi CREATE TABLE ifadesiyle BIREBIR ayni
 * olacak sekilde elle yazildi (exportSchema kapaliyken onceden bir semanin
 * gercek JSON ciktisi alinmamisti, bu yuzden entity tanimindan turetildi).
 * Boylece kullanicilar APK'yi ESKI SURUMU KALDIRMADAN guncelleyebilir ve
 * `sessions` tablosundaki verileri KAYBETMEZ (onceki surumde
 * fallbackToDestructiveMigration() kullanildigi icin bu garanti yoktu).
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `schedule_slots` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`day` INTEGER NOT NULL, " +
                "`start_time` TEXT NOT NULL, " +
                "`end_time` TEXT NOT NULL, " +
                "`kind` TEXT NOT NULL)"
        )
    }
}

/** v2 -> v3: `blocked_apps` tablosu eklendi (Uygulama Kilidi). MIGRATION_1_2'deki
 * ayni yontem: entity tanimindan elle turetilen CREATE TABLE, veri kaybi yok. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `blocked_apps` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`package_name` TEXT NOT NULL, " +
                "`app_label` TEXT NOT NULL, " +
                "`daily_limit_minutes` INTEGER, " +
                "`study_hours_only` INTEGER NOT NULL)"
        )
    }
}

/** v3 -> v4: `keystroke_logs` tablosu eklendi (Klavye Takibi, varsayilan KAPALI). */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `keystroke_logs` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`package_name` TEXT NOT NULL, " +
                "`app_label` TEXT NOT NULL, " +
                "`text` TEXT NOT NULL, " +
                "`timestamp` INTEGER NOT NULL)"
        )
    }
}

/** v4 -> v5: `safe_zones` (birden fazla Guvenli Bolge) ve `location_logs`
 * (surekli konum gecmisi - gunluk yedek e-postasina "sadece anlik degil tum
 * konum" eklenebilsin diye) tablolari eklendi. */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `safe_zones` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`lat` REAL NOT NULL, " +
                "`lng` REAL NOT NULL, " +
                "`radius_meters` REAL NOT NULL, " +
                "`enabled` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `location_logs` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`lat` REAL NOT NULL, " +
                "`lng` REAL NOT NULL, " +
                "`timestamp` INTEGER NOT NULL)"
        )
    }
}

@Database(
    entities = [
        SessionEntity::class, ScheduleSlotEntity::class, BlockedAppEntity::class,
        KeystrokeLogEntity::class, SafeZoneEntity::class, LocationLogEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun keystrokeLogDao(): KeystrokeLogDao
    abstract fun safeZoneDao(): SafeZoneDao
    abstract fun locationLogDao(): LocationLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "study_tracker.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build().also { INSTANCE = it }
            }
        }
    }
}
