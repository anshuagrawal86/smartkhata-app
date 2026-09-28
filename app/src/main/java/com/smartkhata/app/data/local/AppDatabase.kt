package com.smartkhata.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.smartkhata.app.data.local.converters.Converters
import com.smartkhata.app.data.local.dao.ContactDao
import com.smartkhata.app.data.local.dao.EntryDao
import com.smartkhata.app.data.local.dao.ReminderDao
import com.smartkhata.app.data.local.entity.ContactEntity
import com.smartkhata.app.data.local.entity.EntryEntity
import com.smartkhata.app.data.local.entity.ReminderEntity

@Database(
    entities = [
        ContactEntity::class,
        EntryEntity::class,
        ReminderEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun entryDao(): EntryDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_khata.db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
