package com.hctt.clubmembers.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [MemberEntity::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class MemberDatabase : RoomDatabase() {
    abstract fun memberDao(): MemberDao
}
