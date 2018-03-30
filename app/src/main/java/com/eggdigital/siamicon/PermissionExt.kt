package com.eggdigital.siamicon

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.provider.Settings
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AppCompatActivity
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener


inline fun Fragment.requestPermission(func: Builder.() -> Unit) {
    Builder(activity!!).apply(func).check()
}

inline fun AppCompatActivity.requestPermission(func: Builder.() -> Unit) {
    Builder(this).apply(func).check()
}

class Builder(private val activity: FragmentActivity) {

    companion object {
        fun permissionSettingsIntent(context: Context): Intent {
            return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
    }

    private val permissions = mutableListOf<String>()
    private var grantedFunc: (() -> Unit)? = null
    private var permissionDeniedFunc: ((List<PermissionDeniedResponse>) -> Unit)? = null

    fun with(permissionName: String) {
        permissions.add(permissionName)
    }

    infix fun allPermissionGranted(grantedFunc: () -> Unit) {
        this.grantedFunc = grantedFunc
    }

    infix fun permissionDenied(permissionDeniedFunc: (List<PermissionDeniedResponse>) -> Unit) {
        this.permissionDeniedFunc = permissionDeniedFunc
    }

    fun check() {
        Handler().post {
            Dexter.withActivity(activity)
                    .withPermissions(permissions)
                    .withListener(object : MultiplePermissionsListener {
                        override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                            if (report.areAllPermissionsGranted()) {
                                grantedFunc?.invoke()
                            } else {
                                permissionDeniedFunc?.invoke(report.deniedPermissionResponses)
                            }
                        }

                        override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>, token: PermissionToken) {
                            token.continuePermissionRequest()
                        }

                    })
                    .check()
        }
    }
}
