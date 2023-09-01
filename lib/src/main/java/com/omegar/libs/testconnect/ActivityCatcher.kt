package com.omegar.libs.testconnect

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Created by Anton Knyazev on 30.08.2023.
 * Copyright (c) 2023 Omega https://omega-r.com
 */
class ActivityCatcher(context: Context) : ActivityLifecycleCallbacks {

    private val mutableFlow = MutableStateFlow<Activity?>(null)

    val flow = mutableFlow.asStateFlow()

    init {
        (context.applicationContext as? Application)?.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        // nothing
    }

    override fun onActivityStarted(activity: Activity) {
        // nothing
    }

    override fun onActivityResumed(activity: Activity) {
        mutableFlow.value = activity
    }

    override fun onActivityPaused(activity: Activity) {
        mutableFlow.value = null
    }

    override fun onActivityStopped(activity: Activity) {
        // nothing
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        // nothing
    }

    override fun onActivityDestroyed(activity: Activity) {
        // nothing
    }
}