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
    // 최초 설치 시 assets/lotto.db(전 회차 내장)로 생성. 이후 신규 회차는 동기화로 추가
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LottoDatabase =
        Room.databaseBuilder(context, LottoDatabase::class.java, LottoDatabase.NAME)
            .createFromAsset(LottoDatabase.ASSET)
            .build()

    @Provides
    fun provideDrawDao(db: LottoDatabase): DrawDao = db.drawDao()

    @Provides
    fun provideTicketDao(db: LottoDatabase): TicketDao = db.ticketDao()
}
