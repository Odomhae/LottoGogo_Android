package com.odom.lottogogo.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.odom.lottogogo.data.local.entity.Draw
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/** Firestore `draws` 컬렉션 (문서 ID = 회차 문자열). 클라이언트는 읽기만 한다. */
class FirestoreDrawDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) : RemoteDrawDataSource {

    override suspend fun drawsAfter(afterRound: Int): List<Draw> =
        firestore.collection(COLLECTION)
            .whereGreaterThan(FIELD_ROUND, afterRound)
            .orderBy(FIELD_ROUND, Query.Direction.ASCENDING)
            .get()
            .await()
            .documents
            .mapNotNull { it.data?.toDrawOrNull() }

    private companion object {
        const val COLLECTION = "draws"
        const val FIELD_ROUND = "round"
    }
}
