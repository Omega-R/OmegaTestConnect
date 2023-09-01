package com.omegar.testconnect

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Job

/**
 * Created by Anton Knyazev on 07.07.2023.
 * Copyright (c) 2023 Omega https://omega-r.com
 */
class MainActivity : AppCompatActivity() {

    private var job: Job? = null
        set(value) {
            field?.cancel()
            field = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.button_log).setOnClickListener {
            Log.v("TAG", "Test log")
            throw NullPointerException()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job = null
    }

}