package com.odom.lottogogo.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.odom.lottogogo.data.local.entity.MyTicket
import kotlinx.coroutines.flow.Flow

@Dao
interface TicketDao {
    @Query("SELECT * FROM MyTicket ORDER BY round DESC, createdAt DESC")
    fun all(): Flow<List<MyTicket>>

    @Query("SELECT * FROM MyTicket WHERE id = :id")
    fun byId(id: Long): Flow<MyTicket?>

    @Query("SELECT * FROM MyTicket WHERE round = :round AND rank IS NULL")
    suspend fun pending(round: Int): List<MyTicket>

    @Insert suspend fun insert(tickets: List<MyTicket>)
    @Update suspend fun update(ticket: MyTicket)
    @Delete suspend fun delete(ticket: MyTicket)
}
