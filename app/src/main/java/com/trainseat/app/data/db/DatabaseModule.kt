package com.trainseat.app.data.db

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "train_seat_alert.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideAlertConfigDao(db: AppDatabase): AlertConfigDao = db.alertConfigDao()

    @Provides
    fun provideCheckResultDao(db: AppDatabase): CheckResultDao = db.checkResultDao()
}
