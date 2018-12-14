package com.gtgalone.gulliver

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.gtgalone.gulliver.models.Location
import org.jetbrains.anko.doAsync

class SplashActivity : AppCompatActivity() {
  companion object {
    const val TAG = "SplashActivity"
    const val CURRENT_LOCATION = "CurrentLocation"
  }
    /**
   * Provides the entry point to the Fused Location Provider API.
   */
  private var mFusedLocationClient: FusedLocationProviderClient? = null

  public override fun onStart() {
    super.onStart()
    if (!checkPermissions()) {
      Log.d("test", "no")
      requestPermissions()
    } else {
      Log.d("test", "yes")
      getLastLocation()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_splash)

    try {
      mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    } catch (e: InterruptedException) {
      e.printStackTrace()
    }
  }

  /**
   * Provides a simple way of getting a device's location and is well suited for
   * applications that do not require a fine-grained location and that do not need location
   * updates. Gets the best and most recent location currently available, which may be null
   * in rare cases when a location is not available.
   *
   *
   * Note: this method should be called after location permission has been granted.
   */
  @SuppressLint("MissingPermission")
  private fun getLastLocation() {
    mFusedLocationClient!!.lastLocation
      .addOnCompleteListener(this) { task ->
        val uid = FirebaseAuth.getInstance().uid

        val intent: Intent

        if (uid == null) {
          intent = Intent(this@SplashActivity, SignInActivity::class.java)
        } else {
          intent = Intent(this@SplashActivity, MainActivity::class.java)
        }
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)

        if (task.isSuccessful && task.result != null) {

          doAsync {

            val geo = Geocoder(this@SplashActivity)
            val mLastLocation = task.result!!

            val latitude = mLastLocation.latitude
            val longitude = mLastLocation.longitude

            val firstLocation = geo.getFromLocation(latitude, longitude, 10)[0]

            val countryCode = firstLocation.countryCode
            val adminArea = firstLocation.adminArea
            val locality = firstLocation.locality

//            FirebaseDatabase.getInstance().getReference("servers/${}")

            intent.putExtra(CURRENT_LOCATION, Location(countryCode, adminArea, locality))
          }

        } else {
          showMessage(getString(R.string.no_location_detected))
        }
        startActivity(intent)
      }
  }

  /**
   * Shows a [] using `text`.
   * @param text The Snackbar text.
   */
  private fun showMessage(text: String) {
    Log.d("test", "show mes")
    val container = findViewById<View>(R.id.main_activity_layout)
    if (container != null) {
      Toast.makeText(this@SplashActivity, text, Toast.LENGTH_LONG).show()
    }
  }

  /**
   * Shows a [].
   * @param mainTextStringId The id for the string resource for the Snackbar text.
   * *
   * @param actionStringId   The text of the action item.
   * *
   * @param listener         The listener associated with the Snackbar action.
   */
  private fun showSnackbar(mainTextStringId: Int, actionStringId: Int,
                           listener: View.OnClickListener) {
    Toast.makeText(this@SplashActivity, getString(mainTextStringId), Toast.LENGTH_LONG).show()
  }

  /**
   * Return the current state of the permissions needed.
   */
  private fun checkPermissions(): Boolean {

    val permissionState = ContextCompat.checkSelfPermission(this,
      Manifest.permission.ACCESS_COARSE_LOCATION)
    return permissionState == PackageManager.PERMISSION_GRANTED
  }

  private fun startLocationPermissionRequest() {
    ActivityCompat.requestPermissions(this@SplashActivity,
      arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
      MainActivity.REQUEST_PERMISSIONS_REQUEST_CODE
    )
  }

  private fun requestPermissions() {
    val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
      Manifest.permission.ACCESS_COARSE_LOCATION)
    // Provide an additional rationale to the user. This would happen if the user denied the
    // request previously, but didn't check the "Don't ask again" checkbox.
    if (shouldProvideRationale) {
      Log.i(TAG, "Displaying permission rationale to provide additional context.")
      showSnackbar(R.string.permission_rationale, android.R.string.ok,
        View.OnClickListener {
          // Request permission
          startLocationPermissionRequest()
        })
    } else {
      Log.i(TAG, "Requesting permission")
      // Request permission. It's possible this can be auto answered if device policy
      // sets the permission in a given state or the user denied the permission
      // previously and checked "Never ask again".
      startLocationPermissionRequest()
    }
  }

  /**
   * Callback received when a permissions request has been completed.
   */
  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                          grantResults: IntArray) {
    Log.i(TAG, "onRequestPermissionResult")
    if (requestCode == MainActivity.REQUEST_PERMISSIONS_REQUEST_CODE) {
      if (grantResults.isEmpty()) {
        // If user interaction was interrupted, the permission request is cancelled and you
        // receive empty arrays.
        Log.i(TAG, "User interaction was cancelled.")
      } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        // Permission granted.
        getLastLocation()
      } else {
        // Permission denied.
        // Notify the user via a SnackBar that they have rejected a core permission for the
        // app, which makes the Activity useless. In a real app, core permissions would
        // typically be best requested during a welcome-screen flow.
        // Additionally, it is important to remember that a permission might have been
        // rejected without asking the user for permission (device policy or "Never ask
        // again" prompts). Therefore, a user interface affordance is typically implemented
        // when permissions are denied. Otherwise, your app could appear unresponsive to
        // touches or interactions which have required permissions.
        showSnackbar(R.string.permission_denied_explanation, R.string.settings,
          View.OnClickListener {
            // Build intent that displays the App settings screen.
            val intent = Intent()
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            val uri = Uri.fromParts("package",
              BuildConfig.APPLICATION_ID, null)
            intent.data = uri
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
          })
      }
    }
  }

}
