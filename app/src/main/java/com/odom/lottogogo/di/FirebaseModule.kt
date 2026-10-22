package com.odom.lottogogo.di

import android.content.Context
import androidx.room.withTransaction
import androidx.work.WorkManager
import com.google.firebase.firestore.FirebaseFirestore
import com.odom.lottogogo.data.local.LottoDatabase
import com.odom.lottogogo.data.remote.FirestoreDrawDataSource
import com.odom.lottogogo.data.remote.RemoteDrawDataSource
import com.odom.lottogogo.sync.TransactionRunner
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FirebaseModule {

    @Binds
    abstract fun bindRemoteDrawDataSource(impl: FirestoreDrawDataSource): RemoteDrawDataSource

    companion object {
        @Provides
        @Singleton
        fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

        @Provides
        fun provideTransactionRunner(db: LottoDatabase): TransactionRunner = object : TransactionRunner {
            override suspend fun <T> run(block: suspend () -> T): T = db.withTransaction { block() }
        }

        @Provides
        fun provideWorkManager(@ApplicationContext context: Context): WorkManager = WorkManager.getInstance(context)
    }
}
