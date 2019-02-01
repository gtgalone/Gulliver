package com.gtgalone.gulliver

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.gtgalone.gulliver.models.Channel
import com.gtgalone.gulliver.models.MyCity
import com.gtgalone.gulliver.models.City
import com.gtgalone.gulliver.models.User
import org.jetbrains.anko.doAsync

class SplashActivity : AppCompatActivity() {
  companion object {
    const val TAG = "SplashActivity"
    const val CURRENT_CITY = "CurrentCity"
    const val REQUEST_PERMISSIONS_REQUEST_CODE = 34
  }

  private lateinit var databaseReference: DatabaseReference
  private lateinit var locationManager: LocationManager
  private lateinit var nextIntent: Intent

  private val uid = FirebaseAuth.getInstance().uid
  private val db = FirebaseFirestore.getInstance()
  private lateinit var userRef: DatabaseReference

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_splash)

    if (uid == null) {
      nextIntent = Intent(this@SplashActivity, SignInActivity::class.java)
    } else {
      nextIntent = Intent(this@SplashActivity, MainActivity::class.java)
      userRef = FirebaseDatabase.getInstance().getReference("/users/$uid")
    }

    databaseReference = FirebaseDatabase.getInstance().getReference("/cities")
    locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
  }

  public override fun onStart() {
    super.onStart()
    val locationRequest = LocationRequest.create()?.apply {
      interval = 10000
      fastestInterval = 5000
      priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    val builder = LocationSettingsRequest.Builder()
      .addLocationRequest(locationRequest!!)
    val client: SettingsClient = LocationServices.getSettingsClient(this)
    val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

    task.addOnSuccessListener {
      Log.d(TAG, "gps on")
      if (!checkPermissions()) {
        Log.d(TAG, "yes")
        requestPermissions()
      } else {
        Log.d(TAG, "no")
        getLastLocation()
      }
    }

    task.addOnFailureListener {
      Log.d(TAG, "gps off")
      if (it is ResolvableApiException) {
        try {
          it.startResolutionForResult(this@SplashActivity, 1)
        } catch (sendEx: IntentSender.SendIntentException) {

        }
      }
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    Log.d(TAG, requestCode.toString() + resultCode.toString() + data.toString())
    if (resultCode == RESULT_OK) {
      Log.d(TAG, "you hit ok")
      if (!checkPermissions()) {
        Log.d(TAG, "yes")
        requestPermissions()
      } else {
        Log.d(TAG, "no")
        getLastLocation()
      }
    } else {
      Log.d(TAG, "you hit no thanks")
      finish()
    }
  }

  private val locationListener: android.location.LocationListener = object : LocationListener {
    override fun onLocationChanged(location: android.location.Location?) {
      Log.d(TAG, "loc cha")
      if (location != null) {
        Log.d(TAG, location.latitude.toString() + "," + location.longitude.toString())
        changeActivityWithLocation(location)
        locationManager.removeUpdates(this)
      }
    }
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderDisabled(provider: String?) {}
    override fun onProviderEnabled(provider: String?) {}
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

    val location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
    if (location == null) {
      Log.d(TAG, "location null")
      changeActivityWithLocation(Location(""))
      locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 0F, locationListener)
    } else {
      changeActivityWithLocation(location)
    }
  }

  private fun changeActivityWithLocation(location: android.location.Location) {
    doAsync {
      val geo = Geocoder(this@SplashActivity)

      val latitude = location.latitude
      val longitude = location.longitude

      val locationInformation = geo.getFromLocation(51.509553, -0.131421, 10)[0]

      val countryCode = locationInformation.countryCode
      val adminArea = locationInformation.adminArea
      val locality = locationInformation.locality ?: locationInformation.adminArea

      val name = countryCode.replace(" ", "").toLowerCase() + "-" +
          adminArea.replace(" ", "").toLowerCase() + "-" +
          locality.replace(" ", "").toLowerCase()

      getCity(City(name, countryCode, adminArea, locality))
    }
  }

  private fun getCity(city: City) {
    db.collection("cities").whereEqualTo("id", city.id).get()
      .addOnSuccessListener {
        if (it.isEmpty) {
          db.collection("cities").document(city.id).set(City(city.id, city.countryCode, city.adminArea, city.locality))
          .addOnSuccessListener {
            val channels = arrayListOf("general", "trade")
            for (channel in channels) {
              Log.d("test", channel)
              db.collection("cities").document(city.id)
                .collection("channels").document(channel)
                .set(Channel(channel))

              if (channel == "general") {

                if (uid != null) {
                  userRef.child("currentCity").setValue(city.id)
                  userRef.child("currentChannel").setValue("general")

                  addCityIfNotExist(city, uid)
                }

                nextIntent.putExtra(CURRENT_CITY, MyCity(
                  city.id,
                  city.countryCode,
                  city.adminArea,
                  city.locality,
                  System.currentTimeMillis()
                ))

                changeActivity()
              }
            }
          }
        } else {
          val cityInfo = it.documents.first().toObject(City::class.java) ?: return@addOnSuccessListener
          if (uid != null) {
            userRef.child("currentCity").setValue(cityInfo.id)
            userRef.child("currentChannel").setValue("general")

            addCityIfNotExist(cityInfo, uid)
          } else {
            nextIntent.putExtra(CURRENT_CITY, MyCity(
              cityInfo.id,
              cityInfo.countryCode,
              cityInfo.adminArea,
              cityInfo.locality,
              System.currentTimeMillis()
            ))

            changeActivity()
          }
        }
      }
  }

  private fun addCityIfNotExist(city: City, uid: String) {
    val myCitiesRef = FirebaseDatabase.getInstance().getReference("/users/$uid/cities")
    FirebaseDatabase.getInstance().getReference("/users/$uid/cities/${city.id}")
      .addListenerForSingleValueEvent(object: ValueEventListener {
        override fun onDataChange(p0: DataSnapshot) {
          if (!p0.hasChildren()) {
            myCitiesRef.child(city.id).setValue(MyCity(
              city.id,
              city.countryCode,
              city.adminArea,
              city.locality,
              System.currentTimeMillis()
            ))
          } else {
            val myCity = p0.getValue(MyCity::class.java) ?: return

            nextIntent.putExtra(CURRENT_CITY, MyCity(
              myCity.id,
              myCity.countryCode,
              myCity.adminArea,
              myCity.locality,
              myCity.timestamp
            ))

            changeActivity()
          }
        }
        override fun onCancelled(p0: DatabaseError) {}
      })
  }

  private fun changeActivity() {
    startActivity(nextIntent)
    finish()
  }

  /**
   * Shows a [] using `text`.
   * @param text The Snackbar text.
   */
  private fun showMessage(text: String) {
    Log.d(TAG, "show mes")
    Toast.makeText(this@SplashActivity, text, Toast.LENGTH_LONG).show()
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
    Log.d(TAG, "check per")
    val permissionState = ContextCompat.checkSelfPermission(this@SplashActivity,
      Manifest.permission.ACCESS_COARSE_LOCATION)
    return permissionState == PackageManager.PERMISSION_GRANTED
  }

  private fun startLocationPermissionRequest() {
    Log.d(TAG, "start req per")
    ActivityCompat.requestPermissions(this@SplashActivity,
      arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
      REQUEST_PERMISSIONS_REQUEST_CODE
    )
  }

  private fun requestPermissions() {
    val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this@SplashActivity,
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
