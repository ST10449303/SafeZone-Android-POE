package com.example.safezone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SOSActiveActivity : AppCompatActivity() {

    private lateinit var tvBack: TextView
    private lateinit var tvUserName: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvTrustedContacts: TextView

    private lateinit var btnShareLocation: Button
    private lateinit var btnEmergencyServices: Button
    private lateinit var btnTrustedContact: Button
    private lateinit var btnCancelSOS: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var sosDocumentId: String? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_sos_active)

        // ================================
        // CONNECT XML CONTROLS
        // ================================

        tvBack = findViewById(R.id.tvBack)
        tvUserName = findViewById(R.id.tvUserName)
        tvLocation = findViewById(R.id.tvLocation)
        tvTime = findViewById(R.id.tvTime)
        tvTrustedContacts = findViewById(R.id.tvTrustedContacts)

        btnShareLocation = findViewById(R.id.btnShareLocation)
        btnEmergencyServices = findViewById(R.id.btnEmergencyServices)
        btnTrustedContact = findViewById(R.id.btnTrustedContact)
        btnCancelSOS = findViewById(R.id.btnCancelSOS)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // ================================
        // BACK ARROW
        // ================================

        tvBack.setOnClickListener {
            finish()
        }

        // ================================
        // CHECK LOGIN
        // ================================

        val currentUser = auth.currentUser

        if (currentUser == null) {

            Toast.makeText(
                this,
                "Please log in first.",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        // ================================
        // LOAD USER INFORMATION
        // ================================

        loadUserInformation(currentUser.uid)

        // ================================
        // ACTIVATION TIME
        // ================================

        val currentTime = SimpleDateFormat(
            "dd MMM yyyy, HH:mm",
            Locale.getDefault()
        ).format(Date())

        tvTime.text = "Time Activated: $currentTime"

        // ================================
        // CREATE SOS RECORD
        // ================================

        createSOSRecord(currentUser.uid)

        // ================================
        // SHARE LOCATION
        // ================================

        btnShareLocation.setOnClickListener {
            shareLocation()
        }

        // ================================
        // EMERGENCY SERVICES
        // ================================

        btnEmergencyServices.setOnClickListener {
            callEmergencyServices()
        }

        // ================================
        // TRUSTED CONTACT
        // ================================

        btnTrustedContact.setOnClickListener {
            callTrustedContact(currentUser.uid)
        }

        // ================================
        // CANCEL SOS
        // ================================

        btnCancelSOS.setOnClickListener {
            cancelSOS()
        }
    }

    // ============================================================
    // LOAD USER INFORMATION
    // ============================================================

    private fun loadUserInformation(userId: String) {

        db.collection("Users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->

                if (document.exists()) {

                    val fullName =
                        document.getString("fullName")

                    if (!fullName.isNullOrEmpty()) {

                        tvUserName.text =
                            "Your Name: $fullName"

                    } else {

                        tvUserName.text =
                            "Your Name: SafeZone User"
                    }

                    val contactName =
                        document.getString("trustedContactName")

                    if (!contactName.isNullOrEmpty()) {

                        tvTrustedContacts.text =
                            "Trusted Contact: $contactName"

                    } else {

                        tvTrustedContacts.text =
                            "Trusted Contact: Not configured"
                    }

                } else {

                    tvUserName.text =
                        "Your Name: SafeZone User"

                    tvTrustedContacts.text =
                        "Trusted Contact: Not configured"
                }
            }
            .addOnFailureListener {

                tvUserName.text =
                    "Your Name: SafeZone User"

                tvTrustedContacts.text =
                    "Trusted Contact: Not configured"

                Toast.makeText(
                    this,
                    "Unable to load your profile.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // ============================================================
    // CREATE SOS RECORD
    // ============================================================

    private fun createSOSRecord(userId: String) {

        val sosData: Map<String, Any> = mapOf(

            "userId" to userId,

            "status" to "Active",

            "type" to "Emergency SOS",

            "activatedAt" to System.currentTimeMillis(),

            "location" to "Location not shared yet",

            "trustedContactsNotified" to false
        )

        db.collection("SOSRequests")
            .add(sosData)
            .addOnSuccessListener { documentReference ->

                sosDocumentId =
                    documentReference.id

                Toast.makeText(
                    this,
                    "SOS request activated.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "SOS activated, but could not save the request.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // ============================================================
    // SHARE LOCATION
    // ============================================================

    private fun shareLocation() {

        // Check location permission

        val fineLocationGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (!fineLocationGranted && !coarseLocationGranted) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST
            )

            return
        }

        getCurrentLocation()
    }

    // ============================================================
    // GET CURRENT LOCATION
    // ============================================================

    private fun getCurrentLocation() {

        val locationManager =
            getSystemService(LOCATION_SERVICE) as LocationManager

        // Check if GPS is enabled

        val gpsEnabled =
            locationManager.isProviderEnabled(
                LocationManager.GPS_PROVIDER
            )

        val networkEnabled =
            locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
            )

        if (!gpsEnabled && !networkEnabled) {

            Toast.makeText(
                this,
                "Please turn on Location on your phone.",
                Toast.LENGTH_LONG
            ).show()

            try {

                startActivity(
                    Intent(
                        Settings.ACTION_LOCATION_SOURCE_SETTINGS
                    )
                )

            } catch (e: Exception) {

                // Ignore if settings cannot be opened
            }

            return
        }

        // Check permission again

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }

        Toast.makeText(
            this,
            "Getting your current location...",
            Toast.LENGTH_SHORT
        ).show()

        var bestLocation: Location? = null

        // GPS location

        if (gpsEnabled) {

            val gpsLocation =
                locationManager.getLastKnownLocation(
                    LocationManager.GPS_PROVIDER
                )

            if (gpsLocation != null) {
                bestLocation = gpsLocation
            }
        }

        // Network location

        if (networkEnabled) {

            val networkLocation =
                locationManager.getLastKnownLocation(
                    LocationManager.NETWORK_PROVIDER
                )

            if (networkLocation != null) {

                if (
                    bestLocation == null ||
                    networkLocation.accuracy < bestLocation.accuracy
                ) {

                    bestLocation = networkLocation
                }
            }
        }

        if (bestLocation != null) {

            val latitude =
                bestLocation.latitude

            val longitude =
                bestLocation.longitude

            val locationText =
                "Location: $latitude, $longitude"

            tvLocation.text = locationText

            saveLocationToFirebase(
                latitude,
                longitude
            )

            shareLocationWithPhone(
                latitude,
                longitude
            )

        } else {

            Toast.makeText(
                this,
                "Location is not available yet. Please make sure GPS is turned on and try again.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ============================================================
    // SAVE LOCATION TO FIREBASE
    // ============================================================

    private fun saveLocationToFirebase(
        latitude: Double,
        longitude: Double
    ) {

        val documentId =
            sosDocumentId

        if (documentId == null) {

            Toast.makeText(
                this,
                "SOS record is still being created. Please try again.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val locationData: Map<String, Any> =
            mapOf(
                "latitude" to latitude,
                "longitude" to longitude
            )

        db.collection("SOSRequests")
            .document(documentId)
            .update(
                mapOf(
                    "location" to locationData,
                    "locationShared" to true
                )
            )
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Location saved to your SOS request.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "Location found, but could not save it to Firebase.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // ============================================================
    // SHARE LOCATION USING PHONE
    // ============================================================

    private fun shareLocationWithPhone(
        latitude: Double,
        longitude: Double
    ) {

        val mapsLink =
            "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"

        val shareText =
            "SafeZone Emergency Location\n\n" +
                    "My current location is:\n" +
                    mapsLink

        val shareIntent =
            Intent(Intent.ACTION_SEND).apply {

                type = "text/plain"

                putExtra(
                    Intent.EXTRA_TEXT,
                    shareText
                )
            }

        startActivity(
            Intent.createChooser(
                shareIntent,
                "Share SafeZone Location"
            )
        )
    }

    // ============================================================
    // LOCATION PERMISSION RESULT
    // ============================================================

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (
            requestCode ==
            LOCATION_PERMISSION_REQUEST
        ) {

            if (
                grantResults.isNotEmpty() &&
                grantResults.any {
                    it == PackageManager.PERMISSION_GRANTED
                }
            ) {

                getCurrentLocation()

            } else {

                Toast.makeText(
                    this,
                    "Location permission is required to share your location.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ============================================================
    // CALL EMERGENCY SERVICES
    // ============================================================

    private fun callEmergencyServices() {

        try {

            val intent =
                Intent(
                    Intent.ACTION_DIAL,
                    Uri.parse("tel:112")
                )

            startActivity(intent)

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Unable to open the phone dialler.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ============================================================
    // TRUSTED CONTACT
    // ============================================================

    private fun callTrustedContact(
        userId: String
    ) {

        db.collection("Users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->

                val contactName =
                    document.getString(
                        "trustedContactName"
                    )

                val contactPhone =
                    document.getString(
                        "trustedContactPhone"
                    )

                if (
                    !contactName.isNullOrEmpty() &&
                    !contactPhone.isNullOrEmpty()
                ) {

                    openTrustedContactDialler(
                        contactName,
                        contactPhone
                    )

                } else {

                    showTrustedContactDialog(
                        userId
                    )
                }
            }
            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "Unable to load trusted contact.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // ============================================================
    // ADD TRUSTED CONTACT
    // ============================================================

    private fun showTrustedContactDialog(
        userId: String
    ) {

        val layout =
            LinearLayout(this)

        layout.orientation =
            LinearLayout.VERTICAL

        layout.setPadding(
            50,
            10,
            50,
            10
        )

        val nameInput =
            EditText(this)

        nameInput.hint =
            "Contact Name"

        layout.addView(
            nameInput
        )

        val phoneInput =
            EditText(this)

        phoneInput.hint =
            "Phone Number"

        phoneInput.inputType =
            InputType.TYPE_CLASS_PHONE

        layout.addView(
            phoneInput
        )

        val dialog =
            AlertDialog.Builder(this)
                .setTitle(
                    "Add Trusted Contact"
                )
                .setMessage(
                    "Enter a trusted contact who can be called during an emergency."
                )
                .setView(layout)
                .setNegativeButton(
                    "Cancel",
                    null
                )
                .setPositiveButton(
                    "Save & Call",
                    null
                )
                .create()

        dialog.setOnShowListener {

            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                val name =
                    nameInput.text
                        .toString()
                        .trim()

                val phone =
                    phoneInput.text
                        .toString()
                        .trim()

                if (name.isEmpty()) {

                    nameInput.error =
                        "Enter the contact name"

                    return@setOnClickListener
                }

                if (phone.isEmpty()) {

                    phoneInput.error =
                        "Enter the phone number"

                    return@setOnClickListener
                }

                saveTrustedContact(
                    userId,
                    name,
                    phone
                )

                dialog.dismiss()
            }
        }

        dialog.show()
    }

    // ============================================================
    // SAVE TRUSTED CONTACT
    // ============================================================

    private fun saveTrustedContact(
        userId: String,
        name: String,
        phone: String
    ) {

        val contactData: Map<String, Any> =
            mapOf(
                "trustedContactName" to name,
                "trustedContactPhone" to phone
            )

        db.collection("Users")
            .document(userId)
            .update(contactData)
            .addOnSuccessListener {

                tvTrustedContacts.text =
                    "Trusted Contact: $name"

                Toast.makeText(
                    this,
                    "Trusted contact saved.",
                    Toast.LENGTH_SHORT
                ).show()

                openTrustedContactDialler(
                    name,
                    phone
                )
            }
            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "Could not save trusted contact.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // ============================================================
    // OPEN TRUSTED CONTACT DIALLER
    // ============================================================

    private fun openTrustedContactDialler(
        name: String,
        phone: String
    ) {

        try {

            val intent =
                Intent(
                    Intent.ACTION_DIAL,
                    Uri.parse("tel:$phone")
                )

            startActivity(intent)

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Unable to open the phone dialler.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ============================================================
    // CANCEL SOS
    // ============================================================

    private fun cancelSOS() {

        val documentId =
            sosDocumentId

        if (documentId == null) {

            Toast.makeText(
                this,
                "SOS request is still being created.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        db.collection("SOSRequests")
            .document(documentId)
            .update(
                mapOf(
                    "status" to "Cancelled",
                    "cancelledAt" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "SOS has been cancelled.",
                    Toast.LENGTH_SHORT
                ).show()

                val intent =
                    Intent(
                        this,
                        PersonalHomeActivity::class.java
                    )

                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK

                startActivity(intent)

                finish()
            }
            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "Unable to cancel the SOS. Please try again.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}