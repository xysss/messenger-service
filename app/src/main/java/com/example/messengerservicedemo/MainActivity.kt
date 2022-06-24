package com.example.messengerservicedemo

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.messengerservicedemo.ext.job

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        //finish()

        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

}