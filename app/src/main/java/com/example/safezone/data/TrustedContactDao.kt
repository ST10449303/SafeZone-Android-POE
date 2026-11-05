
package com.example.safezone.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrustedContactDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: TrustedContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<TrustedContactEntity>)

    @Query("""
        SELECT * FROM trusted_contacts
        WHERE userId = :userId
        AND syncStatus != 'Deleted Pending Sync'
        ORDER BY fullName ASC
    """)
    fun getContactsForUser(userId: String): Flow<List<TrustedContactEntity>>

    @Query("""
        SELECT * FROM trusted_contacts
        WHERE userId = :userId
        AND syncStatus != 'Deleted Pending Sync'
        ORDER BY fullName ASC
    """)
    suspend fun getContactsForUserOnce(userId: String): List<TrustedContactEntity>

    @Query("""
        SELECT * FROM trusted_contacts
        WHERE userId = :userId
        AND syncStatus = 'Pending Sync'
        ORDER BY createdAt ASC
    """)
    suspend fun getPendingSyncContacts(userId: String): List<TrustedContactEntity>

    @Query("""
        SELECT * FROM trusted_contacts
        WHERE userId = :userId
        AND syncStatus = 'Deleted Pending Sync'
    """)
    suspend fun getPendingDeletions(userId: String): List<TrustedContactEntity>

    @Query("""
        UPDATE trusted_contacts
        SET syncStatus = :syncStatus
        WHERE contactId = :contactId
        AND userId = :userId
    """)
    suspend fun updateSyncStatus(
        contactId: String,
        userId: String,
        syncStatus: String
    )

    @Query("""
        UPDATE trusted_contacts
        SET syncStatus = 'Deleted Pending Sync'
        WHERE contactId = :contactId
        AND userId = :userId
    """)
    suspend fun markContactDeleted(
        contactId: String,
        userId: String
    )

    @Query("""
        DELETE FROM trusted_contacts
        WHERE contactId = :contactId
        AND userId = :userId
    """)
    suspend fun deleteContact(
        contactId: String,
        userId: String
    )

    @Query("DELETE FROM trusted_contacts WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
