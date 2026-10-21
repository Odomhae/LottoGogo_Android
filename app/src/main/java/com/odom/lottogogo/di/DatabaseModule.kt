package com.odom.lottogogo.di

import android.content.Context
import androidx.room.Room
import com.odom.lottogogo.data.local.LottoDatabase
import com.odom.lottogogo.data.local.dao.DrawDao
import com.odom.lottogogo.data.local.dao.TicketDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    // Step 9에서 createFromAsset("lotto.db") 적용
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LottoDatabase =
        Room.databaseBuilder(context, LottoDatabase::class.java, LottoDatabase.NAME).build()

    @Provides
    fun provideDrawDao(db: LottoDatabase): DrawDao = db.drawDao()

    @Provides
    fun provideTicketDao(db: LottoDatabase): TicketDao = db.ticketDao()
}
