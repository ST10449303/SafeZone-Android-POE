package com.example.safezone.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incident_reports")
data class IncidentReportEntity(

    // Unique ID for the report.
    // This ID will remain the same during synchronisation.
    @PrimaryKey
    val localId: String,

    // User who submitted the report.
    val userId: String,

    // Campus and incident details.
    @ColumnInfo(defaultValue = "''")
    val campus: String = "",

    val incidentType: String,

    val description: String,

    val location: String,

    // GPS coordinates.
    val latitude: Double,

    val longitude: Double,

    // Date and time of the incident report.
    val dateTime: String,

    // Report status.
    val status: String = "Submitted",

    // Evidence file paths stored on the device.
    val photoEvidence: String = "",

    val videoEvidence: String = "",

    @ColumnInfo(defaultValue = "''")
    val audioEvidence: String = "",

    val fileEvidence: String = "",

    // Original evidence filenames.
    val photoName: String = "",

    val videoName: String = "",

    val fileName: String = "",

    // Timestamp when the report was created.
    val createdAt: Long,

    // Pending Sync or Synced.
    val syncStatus: String = "Pending Sync"
)