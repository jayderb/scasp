package com.scasp.mobile

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set the dashboard layout here.
        // Explicit intent keeps navigation inside the application.
        // findViewById<Button>(R.id.openCourses).setOnClickListener {
        //     startActivity(Intent(this, CourseActivity::class.java))
        // }
    }
}
