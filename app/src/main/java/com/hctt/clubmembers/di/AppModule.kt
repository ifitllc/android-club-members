package com.hctt.clubmembers.di

import android.content.Context
import androidx.room.Room
import com.hctt.clubmembers.data.db.MemberDao
import com.hctt.clubmembers.data.db.MemberDatabase
import com.hctt.clubmembers.data.local.RememberMeStore
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MemberDatabase =
        Room.databaseBuilder(context, MemberDatabase::class.java, "members.db")
            .addMigrations(MIGRATION_2_3)
            .build()

    @Provides
    fun provideMemberDao(db: MemberDatabase): MemberDao = db.memberDao()

    @Provides
    @Singleton
    fun provideRememberMeStore(@ApplicationContext context: Context): RememberMeStore = RememberMeStore(context)
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE members ADD COLUMN paymentAmount REAL")
    }
}
