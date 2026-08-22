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

@Database(entities = [SessionEntity::class, ScheduleSlotEntity::class], version = 2, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun scheduleDao(): ScheduleDao

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
                    .addMigrations(MIGRATION_1_2)
                    .build().also { INSTANCE = it }
            }
        }
    }
}
