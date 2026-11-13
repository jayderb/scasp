package com.scasp.mobile.ui.campus

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.scasp.mobile.R
import com.scasp.mobile.data.model.CampusServiceItem
import com.scasp.mobile.databinding.ActivityCampusBinding
import com.scasp.mobile.ui.common.BaseActivity
import com.scasp.mobile.ui.common.Row
import com.scasp.mobile.ui.common.RowAdapter
import com.scasp.mobile.ui.common.repository
import com.scasp.mobile.util.Geo
import kotlinx.coroutines.launch

/**
 * Campus services with map coordinates.
 * - "Open in map" = the app's one IMPLICIT intent (ACTION_VIEW geo:): the user's own map app draws the map and
 *   directions, so SCASP needs no map SDK, API key or location permission for it. Browser fallback if no map app.
 * - "Show my position" is the only place location permission is requested, and only when tapped.
 */
class CampusServicesActivity : BaseActivity() {
    private lateinit var b: ActivityCampusBinding
    private val vm: CampusViewModel by viewModels { viewModelFactory { initializer { CampusViewModel(repository()) } } }
    private var cancel: CancellationSignal? = null

    private val askLocation = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) { vm.onPermissionGranted(); fetchLocation() }
        else vm.onPermissionDenied(permanently = !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityCampusBinding.inflate(layoutInflater)
        setContentView(b.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val adapter = RowAdapter(onAction = { row -> service(row.key)?.let(::openInMap) })
        setUpList(b.list, adapter) { vm.refresh() }
        collect(vm.state, b.list, adapter, getString(R.string.empty_campus), vm::messageShown) { s ->
            Row(s.id, s.name, "${s.description}\n${s.latitude}, ${s.longitude}" + if (s.illustrative) " \u00b7 illustrative" else "",
                action = getString(R.string.open_in_map))
        }
        b.showPosition.setOnClickListener { onShowPositionClicked() }
        b.openSettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { vm.location.collect { b.locationText.text = it; b.locationText.visibility = if (it == null) View.GONE else View.VISIBLE } }
                launch { vm.permanentlyDenied.collect { b.openSettings.visibility = if (it) View.VISIBLE else View.GONE } }
            }
        }
        vm.loadIfNeeded()
    }

    override fun onStop() { cancel?.cancel(); cancel = null; super.onStop() }

    private fun service(id: String): CampusServiceItem? = vm.state.value.items.firstOrNull { it.id == id }

    /** Implicit intent: any installed map app can handle geo: URIs. */
    private fun openInMap(s: CampusServiceItem) {
        val geo = Intent(Intent.ACTION_VIEW, Uri.parse(Geo.geoUri(s)))
        try {
            startActivity(Intent.createChooser(geo, getString(R.string.open_with, s.name)))
        } catch (e: ActivityNotFoundException) {
            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Geo.webMapUrl(s)))) }
            catch (e2: ActivityNotFoundException) { Toast.makeText(this, R.string.no_map_app, Toast.LENGTH_LONG).show() }
        }
    }

    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun onShowPositionClicked() {
        when {
            hasLocationPermission() -> fetchLocation()
            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION) ->
                AlertDialog.Builder(this).setTitle(R.string.location_why_title).setMessage(R.string.location_why)
                    .setPositiveButton(R.string.continue_label) { _, _ -> requestLocation() }
                    .setNegativeButton(R.string.not_now) { _, _ -> vm.onPermissionDenied(false) }.show()
            else -> requestLocation()
        }
    }

    private fun requestLocation() =
        askLocation.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))

    /** One-shot position; the system delivers it on the main thread without blocking it. */
    @SuppressLint("MissingPermission")   // checked by hasLocationPermission() before every call
    private fun fetchLocation() {
        if (!hasLocationPermission()) return
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!LocationManagerCompat.isLocationEnabled(lm)) { vm.onLocationUnavailable(getString(R.string.location_off)); return }
        val provider = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).firstOrNull {
            lm.isProviderEnabled(it) && (it != LocationManager.GPS_PROVIDER ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        } ?: LocationManager.NETWORK_PROVIDER
        vm.onLocationUnavailable(getString(R.string.locating))
        cancel?.cancel()
        cancel = CancellationSignal().also { signal ->
            LocationManagerCompat.getCurrentLocation(lm, provider, signal, ContextCompat.getMainExecutor(this)) { loc: Location? ->
                val l = loc ?: lm.getLastKnownLocation(provider)
                if (l != null) vm.onLocation(l.latitude, l.longitude, if (l.hasAccuracy()) l.accuracy else null)
                else vm.onLocationUnavailable(getString(R.string.location_unknown))
            }
        }
    }
}
