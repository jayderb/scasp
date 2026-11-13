package com.scasp.mobile

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CoursesActivity : AppCompatActivity() {
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState)
  val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(28,40,28,28) }
  root.addView(TextView(this).apply { text="Course catalogue"; textSize=24f })
  listOf("JAVA301 — Advanced Java","DB302 — Advanced Database","HCI308 — Human Computer Interface","ENT309 — Entrepreneurship & Innovation").forEach { item -> root.addView(TextView(this).apply { text=item; textSize=18f; setPadding(0,18,0,18) }) }
  root.addView(TextView(this).apply { text="Demo catalogue. Connect to the SCASP HTTP API for live course data." })
  setContentView(root)
 }
}
