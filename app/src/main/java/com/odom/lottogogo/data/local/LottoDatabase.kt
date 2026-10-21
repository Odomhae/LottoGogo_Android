package com.odom.lottogogo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.odom.lottogogo.data.local.dao.DrawDao
import com.odom.lottogogo.data.local.dao.TicketDao
import com.odom.lottogogo.data.local.entity.Draw
import com.odom.lottogogo.data.local.entity.MyTicket

@Database(entities = [Draw::class, MyTicket::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class LottoDatabase : RoomDatabase() {
    abstract fun drawDao(): DrawDao
    abstract fun ticketDao(): TicketDao

    companion object {
        const val NAME = "lotto.db"
    }
}
