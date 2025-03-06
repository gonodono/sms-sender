package com.gonodono.smssender.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [MessageEntity::class], version = 1, exportSchema = false)
abstract class SmsSenderDatabase : RoomDatabase() {
    abstract val messageDao: MessageDao
}