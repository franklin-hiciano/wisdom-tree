package com.wisdomtree.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.wisdomtree.data.model.*

@Database(
    entities = [TreeSourceEntity::class, RunEntity::class, VarOverrideEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class WisdomTreeDatabase : RoomDatabase() {
    abstract fun treeSourceDao(): TreeSourceDao
    abstract fun runDao(): RunDao
    abstract fun varOverrideDao(): VarOverrideDao

    companion object {
        @Volatile private var INSTANCE: WisdomTreeDatabase? = null

        fun getInstance(context: Context): WisdomTreeDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WisdomTreeDatabase::class.java,
                    "wisdom_tree.db"
                ).build().also { INSTANCE = it }
            }
    }
}
