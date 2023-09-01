package com.omegar.libs.testconnect

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

/**
 * Created by Anton Knyazev on 30.08.2023.
 * Copyright (c) 2023 Omega https://omega-r.com
 */
class ContextCatcher: ContentProvider() {

    override fun onCreate(): Boolean {
        context?.let {
            TestConnector.init(it.applicationContext)
        }

        return false
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri?  = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int  = 0

}