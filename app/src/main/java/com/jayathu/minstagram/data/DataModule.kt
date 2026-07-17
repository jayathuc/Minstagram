package com.jayathu.minstagram.data

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jayathu.minstagram.data.local.MinstagramDatabase
import com.jayathu.minstagram.data.local.SessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE sessions ADD COLUMN reelsWatched INTEGER NOT NULL DEFAULT 0")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MinstagramDatabase =
        Room.databaseBuilder(context, MinstagramDatabase::class.java, "minstagram.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun provideSessionDao(db: MinstagramDatabase): SessionDao = db.sessionDao()
}
