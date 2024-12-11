package com.example.assignments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import org.osmdroid.config.Configuration
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.util.GeoPoint
import android.webkit.WebView
import android.webkit.WebViewClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import androidx.drawerlayout.widget.DrawerLayout

class MainActivity : AppCompatActivity() {
    private val PERMISSIONS_REQUEST_CODE = 123
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var recyclerView: RecyclerView
    private lateinit var webView: WebView
    private lateinit var mapView: MapView
    private lateinit var urlEditText: TextInputEditText
    private lateinit var loadUrlButton: Button
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)

            // Initialize OSMDroid configuration first
            Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

            // Initialize views before checking permissions
            initializeViews()

            if (!hasRequiredPermissions()) {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE)
            } else {
                initializeApp()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showErrorAndLog("Error in onCreate", e)
        }
    }

    private fun initializeViews() {
        try {
            drawerLayout = findViewById(R.id.drawer_layout) ?: throw IllegalStateException("drawer_layout not found")
            navigationView = findViewById(R.id.nav_view) ?: throw IllegalStateException("nav_view not found")
            recyclerView = findViewById(R.id.recycler_view) ?: throw IllegalStateException("recycler_view not found")
            webView = findViewById(R.id.web_view) ?: throw IllegalStateException("web_view not found")
            mapView = findViewById(R.id.map_view) ?: throw IllegalStateException("map_view not found")
            urlEditText = findViewById(R.id.url_edit_text) ?: throw IllegalStateException("url_edit_text not found")
            loadUrlButton = findViewById(R.id.load_url_button) ?: throw IllegalStateException("load_url_button not found")
        } catch (e: Exception) {
            showErrorAndLog("Error initializing views", e)
            throw e
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun initializeApp() {
        try {
            // Initialize location services
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

            // Configure components
            setupRecyclerView()
            configureWebView()
            configureMapView()

            // Setup navigation drawer
            setupNavigationDrawer()

        } catch (e: Exception) {
            e.printStackTrace()
            showErrorAndLog("Error initializing app", e)
        }
    }


    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = MyAdapter(getDummyData())
    }

    private fun configureWebView() {
        webView.webViewClient = WebViewClient()
        webView.settings.apply {
            javaScriptEnabled = true
            allowContentAccess = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
        }
    }

    private fun configureMapView() {
        try {
            mapView.setMultiTouchControls(true)
            mapView.controller.setZoom(15.0)
            getCurrentLocation()
        } catch (e: Exception) {
            showErrorAndLog("Error configuring map", e)
        }
    }

    private fun setupNavigationDrawer() {
        try {
            navigationView.setNavigationItemSelectedListener { menuItem ->
                try {
                    handleNavigation(menuItem)
                    drawerLayout.closeDrawer(GravityCompat.START)
                } catch (e: Exception) {
                    showErrorAndLog("Error in navigation", e)
                }
                true
            }

            urlEditText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    loadUrl()
                    true
                } else false
            }

            loadUrlButton.setOnClickListener { loadUrl() }
        } catch (e: Exception) {
            showErrorAndLog("Error setting up navigation drawer", e)
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val geoPoint = GeoPoint(location.latitude, location.longitude)
                    mapView.controller.setCenter(geoPoint)
                    addMarkerToMap(geoPoint, "You are here!")
                }
            }
        }
    }

    private fun loadUrl() {
        val url = urlEditText.text.toString()
        if (url.isNotEmpty()) {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                webView.loadUrl("https://$url")
            } else {
                webView.loadUrl(url)
            }
            webView.visibility = View.VISIBLE
            urlEditText.visibility = View.GONE
            loadUrlButton.visibility = View.GONE
        }
    }

    private fun addMarkerToMap(location: GeoPoint, title: String) {
        val marker = Marker(mapView)
        marker.position = location
        marker.title = title
        mapView.overlays.add(marker)
        mapView.invalidate()
    }

    private fun handleNavigation(menuItem: MenuItem) {
        recyclerView.visibility = View.GONE
        webView.visibility = View.GONE
        mapView.visibility = View.GONE
        urlEditText.visibility = View.GONE
        loadUrlButton.visibility = View.GONE

        when (menuItem.itemId) {
            R.id.nav_recycler_view -> recyclerView.visibility = View.VISIBLE
            R.id.nav_web_view -> {
                webView.visibility = View.VISIBLE
                urlEditText.visibility = View.VISIBLE
                loadUrlButton.visibility = View.VISIBLE
            }
            R.id.nav_map_view -> mapView.visibility = View.VISIBLE
        }
    }

    private fun getDummyData(): List<String> {
        return listOf("Item 1", "Item 2", "Item 3", "Item 4", "Item 5")
    }

    private fun showErrorAndLog(message: String, error: Exception) {
        val errorMessage = "$message: ${error.message}"
        Log.e("MainActivity", errorMessage, error)
        runOnUiThread {
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                initializeApp()
            } else {
                Toast.makeText(this, "Permissions required for app functionality", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            if (::mapView.isInitialized) {
                mapView.onResume()
            }
        } catch (e: Exception) {
            showErrorAndLog("Error in onResume", e)
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            if (::mapView.isInitialized) {
                mapView.onPause()
            }
        } catch (e: Exception) {
            showErrorAndLog("Error in onPause", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (::mapView.isInitialized) {
                mapView.onDetach()
            }
        } catch (e: Exception) {
            showErrorAndLog("Error in onDestroy", e)
        }
    }
}