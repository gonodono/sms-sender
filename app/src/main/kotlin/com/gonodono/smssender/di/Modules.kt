package com.gonodono.smssender.di

import android.content.Context
import androidx.room.Room
import com.gonodono.smssender.database.SmsSenderDatabase
import com.gonodono.smssender.repository.SmsSenderRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideRepository(
        @ApplicationContext context: Context,
        database: SmsSenderDatabase,
        dispatcher: CoroutineDispatcher
    ): SmsSenderRepository = SmsSenderRepository(context, database, dispatcher)
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SmsSenderDatabase =
        Room.databaseBuilder(
            context,
            SmsSenderDatabase::class.java,
            "sms_sender.db"
        ).build()
}

@Module
@InstallIn(SingletonComponent::class)
object CoroutineModule {

    @Provides
    @Singleton
    fun provideCoroutineDispatcher(): CoroutineDispatcher = Dispatchers.Default
}