
package com.example.emergencyfinder

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FinderActivity : AppCompatActivity() {

    private lateinit var locationHelper: LocationHelper
    private lateinit var overpassHelper: OverpassHelper
    private lateinit var favouriteDao: FavouriteDao

    private lateinit var txtLocation: TextView
    private lateinit var txtStatus: TextView
    private lateinit var categoryGroup: ChipGroup
    private lateinit var radiusGroup: ChipGroup
    private lateinit var recyclerPlaces: RecyclerView

    private lateinit var btnSearch: Button
    private lateinit var btnFavourites: Button

    private var currentLocation: Location? = null
    private var currentPlaces = mutableListOf<Place>()

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val granted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (granted) {
                getCurrentLocation()
            } else {
                txtLocation.text = "Location permission denied"
                Toast.makeText(
                    this,
                    "Please allow location permission to find nearby services",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_finder)

        locationHelper = LocationHelper(this)
        overpassHelper = OverpassHelper()

        favouriteDao =
            FavouriteDatabase.getDatabase(this).favouriteDao()

        txtLocation = findViewById(R.id.txtLocation)
        txtStatus = findViewById(R.id.txtStatus)

        categoryGroup = findViewById(R.id.categoryGroup)
        radiusGroup = findViewById(R.id.radiusGroup)

        recyclerPlaces = findViewById(R.id.recyclerPlaces)

        btnSearch = findViewById(R.id.btnSearch)
        btnFavourites = findViewById(R.id.btnFavourites)

        recyclerPlaces.layoutManager =
            LinearLayoutManager(this)

        btnSearch.setOnClickListener {
            searchNearbyPlaces()
        }

        btnFavourites.setOnClickListener {
            showFavourites()
        }

        checkLocationPermission()
    }

    private fun checkLocationPermission() {

        val finePermission =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarsePermission =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (finePermission || coarsePermission) {
            getCurrentLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun getCurrentLocation() {

        txtLocation.text = "Getting your current location..."

        locationHelper.getCurrentLocation { location ->

            runOnUiThread {

                if (location != null) {
                    currentLocation = location

                    txtLocation.text = String.format(
                        java.util.Locale.getDefault(),
                        "Latitude: %.5f | Longitude: %.5f",
                        location.latitude,
                        location.longitude
                    )
                } else {
                    txtLocation.text =
                        "Unable to get location. Check GPS settings."
                }
            }
        }
    }

    private fun searchNearbyPlaces() {

        val location = currentLocation

        if (location == null) {
            Toast.makeText(
                this,
                "Current location is not available",
                Toast.LENGTH_SHORT
            ).show()

            checkLocationPermission()
            return
        }

        val category = when (categoryGroup.checkedChipId) {
            R.id.chipHospital -> "hospital"
            R.id.chipPharmacy -> "pharmacy"
            R.id.chipPolice -> "police"
            else -> "hospital"
        }

        val radiusKm = when (radiusGroup.checkedChipId) {
            R.id.chip1Km -> 1
            R.id.chip2Km -> 2
            R.id.chip5Km -> 5
            else -> 1
        }

        val radiusMeters = radiusKm * 1000

        txtStatus.text = "Searching nearby services..."
        btnSearch.isEnabled = false

        lifecycleScope.launch {

            try {

                val results = withContext(Dispatchers.IO) {
                    overpassHelper.searchPlaces(
                        location.latitude,
                        location.longitude,
                        radiusMeters,
                        category
                    )
                }

                currentPlaces = results.mapNotNull { place ->

                    val distance = FloatArray(1)

                    Location.distanceBetween(
                        location.latitude,
                        location.longitude,
                        place.latitude,
                        place.longitude,
                        distance
                    )

                    val distanceKm = distance[0] / 1000.0

                    if (distanceKm <= radiusKm) {
                        place.copy(distanceKm = distanceKm)
                    } else {
                        null
                    }

                }.sortedBy { it.distanceKm }.toMutableList()

                if (currentPlaces.isEmpty()) {
                    recyclerPlaces.adapter = null
                    txtStatus.text =
                        "No nearby services found. Try another category or radius."
                } else {
                    txtStatus.text =
                        "${currentPlaces.size} services found within $radiusKm km"

                    recyclerPlaces.adapter = PlaceAdapter(
                        this@FinderActivity,
                        currentPlaces
                    ) { place ->
                        saveFavourite(place)
                    }
                }

            } catch (e: Exception) {

                txtStatus.text =
                    "Unable to load places. Please try again."

                Toast.makeText(
                    this@FinderActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()

            } finally {
                btnSearch.isEnabled = true
            }
        }
    }

    private fun saveFavourite(place: Place) {

        lifecycleScope.launch {

            val favourite = Favourite(
                id = place.id,
                name = place.name,
                latitude = place.latitude,
                longitude = place.longitude,
                phone = place.phone,
                category = place.category
            )

            val alreadySaved =
                favouriteDao.isFavourite(place.id)

            if (alreadySaved) {
                Toast.makeText(
                    this@FinderActivity,
                    "Already in favourites",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                favouriteDao.insertFavourite(favourite)

                Toast.makeText(
                    this@FinderActivity,
                    "Added to favourites!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showFavourites() {

        lifecycleScope.launch {

            val favourites =
                favouriteDao.getAllFavourites()

            if (favourites.isEmpty()) {

                AlertDialog.Builder(this@FinderActivity)
                    .setTitle("My Favourites")
                    .setMessage("No favourite places saved yet.")
                    .setPositiveButton("OK", null)
                    .show()

            } else {

                val names = favourites.map {
                    "${it.name} (${it.category})"
                }.toTypedArray()

                AlertDialog.Builder(this@FinderActivity)
                    .setTitle("My Favourites")
                    .setItems(names) { _, which ->

                        val selected = favourites[which]

                        AlertDialog.Builder(this@FinderActivity)
                            .setTitle(selected.name)
                            .setMessage(
                                "Category: ${selected.category}\n" +
                                        "Phone: ${selected.phone ?: "Not available"}"
                            )
                            .setPositiveButton("Directions") { _, _ ->

                                val uri = android.net.Uri.parse(
                                    "geo:${selected.latitude}," +
                                            "${selected.longitude}?q=" +
                                            "${selected.latitude}," +
                                            "${selected.longitude}"
                                )

                                startActivity(
                                    android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        uri
                                    )
                                )
                            }
                            .setNegativeButton("Remove") { _, _ ->

                                lifecycleScope.launch {
                                    favouriteDao.deleteFavourite(
                                        selected.id
                                    )

                                    Toast.makeText(
                                        this@FinderActivity,
                                        "Favourite removed",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            .setNeutralButton("Close", null)
                            .show()
                    }
                    .setNegativeButton("Close", null)
                    .show()
            }
        }
    }
}
