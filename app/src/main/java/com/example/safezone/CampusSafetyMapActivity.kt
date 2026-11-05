package com.example.safezone

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.max
import kotlin.math.min

class CampusSafetyMapActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var tvBack: ImageView
    private lateinit var tvNotification: ImageView
    private lateinit var layoutCampusSelector: LinearLayout
    private lateinit var tvSelectedCampus: TextView
    private lateinit var tvCampusArrow: TextView
    private lateinit var etSearchLocation: EditText

    private lateinit var btnCompass: ImageView
    private lateinit var btnZoomIn: TextView
    private lateinit var btnZoomOut: TextView
    private lateinit var btnMyLocation: ImageView
    private lateinit var btnGetDirections: AppCompatButton
    private lateinit var mapContainer: FrameLayout

    private lateinit var mapLibrary: TextView
    private lateinit var mapStudentCentre: TextView
    private lateinit var mapLectureHalls: TextView
    private lateinit var mapAdmin: TextView
    private lateinit var mapMainEntrance: TextView

    private var campusName = "Pretoria Campus"
    private var selectedDestination = ""

    private var currentZoom = 1f
    private val minimumZoom = 0.8f
    private val maximumZoom = 1.8f
    private val zoomStep = 0.15f

    private var mapRotation = 0f

    /*
     * These are the actual campus-map elements that should zoom and rotate.
     * The compass, zoom buttons, and location button remain stationary.
     */
    private val mapElements = mutableListOf<View>()

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                openCurrentLocationInMaps()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.campus_map_location_permission_denied),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_campus_safety_map)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initializeViews()
        setupMapElements()
        setupButtons()
        setupBuildingClickListeners()
        setupBackNavigation()
        loadCampusInformation()
    }

    private fun initializeViews() {
        tvBack = findViewById(R.id.tvBack)
        tvNotification = findViewById(R.id.tvNotification)

        layoutCampusSelector = findViewById(R.id.layoutCampusSelector)
        tvSelectedCampus = findViewById(R.id.tvSelectedCampus)
        tvCampusArrow = findViewById(R.id.tvCampusArrow)

        etSearchLocation = findViewById(R.id.etSearchLocation)

        btnCompass = findViewById(R.id.btnCompass)
        btnZoomIn = findViewById(R.id.btnZoomIn)
        btnZoomOut = findViewById(R.id.btnZoomOut)
        btnMyLocation = findViewById(R.id.btnMyLocation)
        btnGetDirections = findViewById(R.id.btnGetDirections)

        mapContainer = findViewById(R.id.mapContainer)

        mapLibrary = findViewById(R.id.mapLibrary)
        mapStudentCentre = findViewById(R.id.mapStudentCentre)
        mapLectureHalls = findViewById(R.id.mapLectureHalls)
        mapAdmin = findViewById(R.id.mapAdmin)
        mapMainEntrance = findViewById(R.id.mapMainEntrance)
    }

    private fun setupMapElements() {
        /*
         * Include the illustrated roads, pathways, building labels,
         * and illustrated current-location marker.
         *
         * Keep the compass and map controls outside this list so
         * they do not rotate or scale with the map.
         */
        for (index in 0 until mapContainer.childCount) {
            val child = mapContainer.getChildAt(index)

            if (
                child.id != R.id.btnCompass &&
                child.id != R.id.btnZoomIn &&
                child.id != R.id.btnZoomOut &&
                child.id != R.id.btnMyLocation
            ) {
                mapElements.add(child)
            }
        }
    }

    private fun setupButtons() {
        tvBack.setOnClickListener {
            finish()
        }

        tvNotification.setOnClickListener {
            openCampusAlerts()
        }

        layoutCampusSelector.setOnClickListener {
            showCampusSelection()
        }

        tvCampusArrow.setOnClickListener {
            showCampusSelection()
        }

        btnZoomIn.setOnClickListener {
            changeZoom(zoomStep)
        }

        btnZoomOut.setOnClickListener {
            changeZoom(-zoomStep)
        }

        btnCompass.setOnClickListener {
            resetMapOrientation()
        }

        btnMyLocation.setOnClickListener {
            requestLocationAndOpenMaps()
        }

        btnGetDirections.setOnClickListener {
            openDirections()
        }

        etSearchLocation.setOnEditorActionListener { _, actionId, event ->
            val isSearchAction =
                actionId == EditorInfo.IME_ACTION_SEARCH ||
                        actionId == EditorInfo.IME_ACTION_DONE ||
                        (event?.keyCode == KeyEvent.KEYCODE_ENTER &&
                                event.action == KeyEvent.ACTION_UP)

            if (isSearchAction) {
                searchCampusLocation()
                true
            } else {
                false
            }
        }

        findViewById<View>(R.id.navHome).setOnClickListener {
            openCampusHome()
        }

        findViewById<View>(R.id.navMap).setOnClickListener {
            // The user is already on the map screen.
        }

        findViewById<View>(R.id.navReports).setOnClickListener {
            openCampusReports()
        }

        findViewById<View>(R.id.navNotifications).setOnClickListener {
            openCampusAlerts()
        }

        findViewById<View>(R.id.navProfile).setOnClickListener {
            openCampusProfile()
        }
    }

    private fun setupBuildingClickListeners() {
        val buildings = listOf(
            mapLibrary,
            mapStudentCentre,
            mapLectureHalls,
            mapAdmin,
            mapMainEntrance
        )

        buildings.forEach { building ->
            building.setOnClickListener {
                selectedDestination = building.text.toString()
                    .replace("\n", " ")
                    .trim()

                etSearchLocation.setText(selectedDestination)

                highlightBuilding(building)

                Toast.makeText(
                    this,
                    getString(
                        R.string.campus_map_building_selected,
                        selectedDestination
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loadCampusInformation() {
        val currentUser = auth.currentUser ?: return

        db.collection("Users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                val storedCampus = document.getString("campus")

                if (!storedCampus.isNullOrBlank()) {
                    campusName = formatCampusName(storedCampus)
                    tvSelectedCampus.text = campusName
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    getString(R.string.campus_map_load_campus_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun formatCampusName(name: String): String {
        val cleanedName = name.trim()

        if (cleanedName.isEmpty()) {
            return getString(R.string.campus_map_default_campus)
        }

        return if (cleanedName.contains("campus", ignoreCase = true)) {
            cleanedName
        } else {
            "$cleanedName Campus"
        }
    }

    private fun changeZoom(amount: Float) {
        currentZoom = (currentZoom + amount).coerceIn(
            minimumZoom,
            maximumZoom
        )

        mapElements.forEach { element ->
            element.scaleX = currentZoom
            element.scaleY = currentZoom
        }

        Toast.makeText(
            this,
            getString(
                R.string.campus_map_zoom_level,
                (currentZoom * 100).toInt()
            ),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun resetMapOrientation() {
        mapRotation = 0f

        mapElements.forEach { element ->
            element.rotation = 0f
        }

        Toast.makeText(
            this,
            getString(R.string.campus_map_orientation_reset),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun searchCampusLocation() {
        val query = etSearchLocation.text.toString().trim()

        hideKeyboard()

        if (query.isEmpty()) {
            Toast.makeText(
                this,
                getString(R.string.campus_map_enter_location),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val buildings = listOf(
            mapLibrary,
            mapStudentCentre,
            mapLectureHalls,
            mapAdmin,
            mapMainEntrance
        )

        val match = buildings.firstOrNull { building ->
            building.text.toString()
                .replace("\n", " ")
                .contains(query, ignoreCase = true)
        }

        if (match != null) {
            selectedDestination = match.text.toString()
                .replace("\n", " ")
                .trim()

            highlightBuilding(match)

            Toast.makeText(
                this,
                getString(
                    R.string.campus_map_location_found,
                    selectedDestination
                ),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            selectedDestination = query

            Toast.makeText(
                this,
                getString(
                    R.string.campus_map_location_not_on_illustration,
                    query
                ),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun highlightBuilding(building: TextView) {
        val buildings = listOf(
            mapLibrary,
            mapStudentCentre,
            mapLectureHalls,
            mapAdmin,
            mapMainEntrance
        )

        buildings.forEach { item ->
            item.alpha = 1f
            item.scaleX = currentZoom
            item.scaleY = currentZoom
        }

        building.alpha = 0.65f
        building.animate()
            .alpha(1f)
            .setDuration(350)
            .start()
    }

    private fun hideKeyboard() {
        val inputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        inputMethodManager.hideSoftInputFromWindow(
            etSearchLocation.windowToken,
            0
        )

        etSearchLocation.clearFocus()
    }

    private fun showCampusSelection() {
        val campuses = arrayOf(
            "Pretoria Campus",
            "Johannesburg Campus",
            "Polokwane Campus",
            "Durban Campus",
            "Bloemfontein Campus"
        )

        AlertDialog.Builder(this)
            .setTitle(R.string.campus_map_select_campus)
            .setSingleChoiceItems(
                campuses,
                campuses.indexOf(campusName).takeIf { it >= 0 } ?: 0
            ) { dialog, which ->
                campusName = campuses[which]
                tvSelectedCampus.text = campusName

                selectedDestination = ""
                etSearchLocation.text.clear()

                dialog.dismiss()

                Toast.makeText(
                    this,
                    getString(
                        R.string.campus_map_campus_selected,
                        campusName
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton(R.string.campus_map_cancel, null)
            .show()
    }

    private fun requestLocationAndOpenMaps() {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION

        when {
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCurrentLocationInMaps()
            }

            else -> {
                locationPermissionLauncher.launch(permission)
            }
        }
    }

    private fun openCurrentLocationInMaps() {
        /*
         * This screen is an illustrated map without geographic
         * coordinates. Open an external map application to show
         * the device's real current position.
         */
        val locationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager

        var lastKnownLocation: Location? = null

        try {
            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestLocationAndOpenMaps()
                return
            }

            lastKnownLocation =
                locationManager.getLastKnownLocation(
                    LocationManager.GPS_PROVIDER
                ) ?: locationManager.getLastKnownLocation(
                    LocationManager.NETWORK_PROVIDER
                )
        } catch (_: SecurityException) {
            // Permission may have been revoked while the activity was open.
        } catch (_: Exception) {
            // Continue to the external map even if no last-known fix exists.
        }

        val uri = if (lastKnownLocation != null) {
            Uri.parse(
                "geo:${lastKnownLocation.latitude}," +
                        "${lastKnownLocation.longitude}?q=" +
                        "${lastKnownLocation.latitude}," +
                        "${lastKnownLocation.longitude}"
            )
        } else {
            Uri.parse("geo:0,0?q=${Uri.encode("My Location")}")
        }

        val intent = Intent(Intent.ACTION_VIEW, uri)

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(
                this,
                getString(R.string.campus_map_no_maps_app),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun openDirections() {
        val destination = selectedDestination.ifBlank {
            etSearchLocation.text.toString().trim()
        }

        val query = if (destination.isNotBlank()) {
            "$campusName $destination"
        } else {
            campusName
        }

        val uri = Uri.parse(
            "geo:0,0?q=${Uri.encode(query)}"
        )

        val intent = Intent(Intent.ACTION_VIEW, uri)

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(
                this,
                getString(R.string.campus_map_no_maps_app),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun openCampusHome() {
        try {
            val intent = Intent(this, CampusHomeActivity::class.java)
            intent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP

            startActivity(intent)
            finish()
        } catch (_: Exception) {
            Toast.makeText(
                this,
                getString(R.string.campus_map_open_home_failed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun openCampusReports() {
        try {
            startActivity(
                Intent(this, CampusReportsActivity::class.java)
            )
        } catch (_: Exception) {
            Toast.makeText(
                this,
                getString(R.string.campus_map_open_reports_failed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun openCampusAlerts() {
        try {
            startActivity(
                Intent(this, CampusAlertsActivity::class.java)
            )
        } catch (_: Exception) {
            Toast.makeText(
                this,
                getString(R.string.campus_map_open_alerts_failed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun openCampusProfile() {
        try {
            startActivity(
                Intent(this, CampusProfileActivity::class.java)
            )
        } catch (_: Exception) {
            Toast.makeText(
                this,
                getString(R.string.campus_map_open_profile_failed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    finish()
                }
            }
        )
    }
}
