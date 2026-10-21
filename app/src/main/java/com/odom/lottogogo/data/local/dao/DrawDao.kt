package com.odom.lottogogo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.odom.lottogogo.data.local.entity.Draw
import kotlinx.coroutines.flow.Flow

@Dao
interface DrawDao {
    @Query("SELECT * FROM Draw ORDER BY round DESC LIMIT 1")
    fun latest(): Flow<Draw?>

    @Query("SELECT * FROM Draw WHERE round = :round")
    fun byRound(round: Int): Flow<Draw?>

    @Query("SELECT round FROM Draw ORDER BY round DESC")
    fun rounds(): Flow<List<Int>>

    @Query("SELECT MAX(round) FROM Draw")
    suspend fun maxRound(): Int?

    @Query("SELECT * FROM Draw WHERE round = :round")
    suspend fun get(round: Int): Draw?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(draws: List<Draw>)

    @Query("""
        SELECT num, COUNT(*) AS cnt FROM (
            SELECT n1 AS num, round FROM Draw UNION ALL
            SELECT n2, round FROM Draw UNION ALL
            SELECT n3, round FROM Draw UNION ALL
            SELECT n4, round FROM Draw UNION ALL
            SELECT n5, round FROM Draw UNION ALL
            SELECT n6, round FROM Draw UNION ALL
            SELECT bonus, round FROM Draw WHERE :includeBonus = 1
        )
        WHERE round > :fromRound
        GROUP BY num ORDER BY num
    """)
    fun countByNumber(fromRound: Int, includeBonus: Boolean): Flow<List<NumberCount>>
}

data class NumberCount(val num: Int, val cnt: Int)
