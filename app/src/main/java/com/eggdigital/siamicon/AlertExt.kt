package com.eggdigital.siamicon

import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity


fun AppCompatActivity.alert(func: AlertDialog.Builder.() -> Unit) {
    AlertDialog.Builder(this).apply(func).show()
}