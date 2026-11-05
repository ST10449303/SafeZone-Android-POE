
package com.example.safezone.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencyRequestDao {

    // Insert or update an emergency request locally.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(request: EmergencyRequestEntity)

    // Observe all emergency requests belonging to a user.
    @Query("""
        SELECT * FROM emergency_requests
        WHERE userId = :userId
        ORDER BY createdAt DESC
    """)
    fun getRequestsForUser(
        userId: String
    ): Flow<List<EmergencyRequestEntity>>

    // Get requests waiting to synchronise with Firestore.
    @Query("""
        SELECT * FROM emergency_requests
        WHERE userId = :userId
        AND syncStatus = 'Pending Sync'
        ORDER BY createdAt ASC
    """)
    suspend fun getPendingRequests(
        userId: String
    ): List<EmergencyRequestEntity>

    // Update the synchronisation status of a request.
    @Query("""
        UPDATE emergency_requests
        SET syncStatus = :newStatus
        WHERE requestId = :requestId
        AND userId = :userId
    """)
    suspend fun updateSyncStatus(
        requestId: String,
        userId: String,
        newStatus: String
    )

    // Observe one request and automatically receive status changes.
    @Query("""
        SELECT * FROM emergency_requests
        WHERE requestId = :requestId
        AND userId = :userId
        LIMIT 1
    """)
    fun observeRequest(
        requestId: String,
        userId: String
    ): Flow<EmergencyRequestEntity?>
}
