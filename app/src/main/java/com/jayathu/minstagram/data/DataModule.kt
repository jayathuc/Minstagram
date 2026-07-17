package com.jayathu.minstagram.data

import android.content.Context
import androidx.room.Room
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

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MinstagramDatabase =
        Room.databaseBuilder(context, MinstagramDatabase::class.java, "minstagram.db").build()

    @Provides
    fun provideSessionDao(db: MinstagramDatabase): SessionDao = db.sessionDao()
}
