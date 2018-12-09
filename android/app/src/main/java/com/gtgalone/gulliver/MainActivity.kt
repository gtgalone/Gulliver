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
import androidx.core.app.ActivityCompat
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.gtgalone.gulliver.models.User
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {
  /**
   * Provides the entry point to the Fused Location Provider API.
   */
  private var mFusedLocationClient: FusedLocationProviderClient? = null

  companion object {
    const val TAG = "LocationProvider"
    const val REQUEST_PERMISSIONS_REQUEST_CODE = 34
    var currentUser: User? = null
  }

  public override fun onStart() {
    super.onStart()
    if (!checkPermissions()) {
      requestPermissions()
    } else {
      getLastLocation()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

    fetchCurrentUser()
  }

  private fun fetchCurrentUser() {
    val uid = FirebaseAuth.getInstance().uid
    val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")

    ref.addListenerForSingleValueEvent(object: ValueEventListener {
      override fun onDataChange(p0: DataSnapshot) {
        currentUser = p0.getValue(User::class.java)
      }

      override fun onCancelled(p0: DatabaseError) {
      }
    })
  }

  private fun changeActivity(activity: Class<*>, reset: Boolean = true) {
    val intent = Intent(this, activity)
    if (reset) intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
  }

  override fun onOptionsItemSelected(item: MenuItem?): Boolean {
    when (item?.itemId) {
      R.id.menu_people -> {
        changeActivity(PeopleActivity::class.java, false)
      }
      R.id.menu_direct_message -> {
        changeActivity(DirectMessagesActivity::class.java, false)
      }
      R.id.menu_sign_out -> {
        FirebaseAuth.getInstance().signOut()
        changeActivity(SignInActivity::class.java)
      }
    }
    return super.onOptionsItemSelected(item)
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.nav_menu, menu)
    return super.onCreateOptionsMenu(menu)
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
              if (task.isSuccessful && task.result != null) {
                Log.d("test", "before doasync")

                doAsync {

                  val geo = Geocoder(this@MainActivity)
                  val mLastLocation = task.result!!

                  val latitude = mLastLocation.latitude
                  val longitude = mLastLocation.longitude

                  val firstLocation = geo.getFromLocation(latitude, longitude, 10)[0]

                  val countryName = firstLocation.countryName
                  val adminArea = firstLocation.adminArea
                  val locality = firstLocation.locality

                  uiThread {
                    latitude_text!!.text = latitude.toString()
                    longitude_text!!.text = longitude.toString()

                    val channelArea: String
                    if (adminArea == locality) {
                      channelArea = getString(R.string.channel_area2, locality, countryName)
                    } else {
                      channelArea = getString(R.string.channel_area1, locality, adminArea, countryName)
                    }

                    location_text!!.text = channelArea
                    supportActionBar?.title = channelArea
                  }
                }

              } else {
                Log.w(TAG, "getLastLocation:exception", task.exception)
                showMessage(getString(R.string.no_location_detected))
              }
            }
  }

  /**
   * Shows a [] using `text`.
   * @param text The Snackbar text.
   */
  private fun showMessage(text: String) {
    val container = findViewById<View>(R.id.main_activity_container)
    if (container != null) {
      Toast.makeText(this@MainActivity, text, Toast.LENGTH_LONG).show()
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
    Toast.makeText(this@MainActivity, getString(mainTextStringId), Toast.LENGTH_LONG).show()
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
    ActivityCompat.requestPermissions(this@MainActivity,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
            REQUEST_PERMISSIONS_REQUEST_CODE)
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
    if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
      if (grantResults.size <= 0) {
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

  fun close(v: View) {
    moveTaskToBack(true)
    exitProcess(-1)
  }

}
