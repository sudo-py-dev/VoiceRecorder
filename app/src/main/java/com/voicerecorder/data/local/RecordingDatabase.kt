package com.voicerecorder.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [RecordingEntity::class], version = 1, exportSchema = false)
abstract class RecordingDatabase : RoomDatabase() {
    abstract val dao: RecordingDao

    companion object {
        private const val DB_NAME = "recordings.db"

        fun build(context: Context): RecordingDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                RecordingDatabase::class.java,
                DB_NAME,
            ).fallbackToDestructiveMigration().build()
        }
    }
}
