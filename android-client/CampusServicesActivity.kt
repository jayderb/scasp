package com.scasp.mobile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class CampusServicesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Example configured campus service coordinates.
        // Use an implicit intent to let the user choose an installed map application.
        val latitude = -15.4167
        val longitude = 28.2833
        val mapUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(Main%20Library)")
        // Trigger this only after the user taps "Open map".
        // startActivity(Intent(Intent.ACTION_VIEW, mapUri))
    }
}
