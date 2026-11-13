package com.scasp.mobile

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState)
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(32,48,32,32) }
        root.addView(TextView(this).apply { text="SCASP Student Portal"; textSize=25f })
        root.addView(TextView(this).apply { text="View academic courses and campus services."; textSize=16f })
        root.addView(Button(this).apply { text="Course catalogue"; setOnClickListener { startActivity(Intent(this@MainActivity,CoursesActivity::class.java)) } })
        root.addView(Button(this).apply { text="Campus services"; setOnClickListener { startActivity(Intent(this@MainActivity,CampusServicesActivity::class.java)) } })
        setContentView(root)
    }
}
