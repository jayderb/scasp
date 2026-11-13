package com.scasp.mobile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CampusServicesActivity : AppCompatActivity() {
 data class Place(val name:String,val lat:Double,val lon:Double)
 private val places=listOf(Place("Library",-15.4167,28.2833),Place("Administration",-15.4172,28.2840),Place("Student Centre",-15.4158,28.2825))
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState)
  val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(28,40,28,28) }
  root.addView(TextView(this).apply { text="Campus services"; textSize=24f })
  places.forEach { p -> root.addView(Button(this).apply { text="Open ${p.name} on map"; setOnClickListener { val uri= Uri.parse("geo:${p.lat},${p.lon}?q=${p.lat},${p.lon}(${Uri.encode(p.name)})"); startActivity(Intent(Intent.ACTION_VIEW,uri)) } }) }
  setContentView(root)
 }
}
