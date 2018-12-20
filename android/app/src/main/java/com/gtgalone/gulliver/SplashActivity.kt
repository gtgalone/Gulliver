package com.gtgalone.gulliver

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Geocoder
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
import com.gtgalone.gulliver.models.Channel
import com.gtgalone.gulliver.models.FavoriteServer
import com.gtgalone.gulliver.models.Server
import org.jetbrains.anko.doAsync

class SplashActivity : AppCompatActivity() {
  companion object {
    const val TAG = "SplashActivity"
    const val CURRENT_SERVER = "CurrentServer"
    const val CURRENT_CHANNEL = "CurrentChannel"
    const val REQUEST_PERMISSIONS_REQUEST_CODE = 34
  }

  private lateinit var databaseReference: DatabaseReference
  private lateinit var locationManager: LocationManager
  private lateinit var nextIntent: Intent

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

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_splash)

    val uid = FirebaseAuth.getInstance().uid

    if (uid == null) {
      nextIntent = Intent(this@SplashActivity, SignInActivity::class.java)
    } else {
      nextIntent = Intent(this@SplashActivity, MainActivity::class.java)
    }

    databaseReference = FirebaseDatabase.getInstance().getReference("/servers")
    locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
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

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
      Log.d(TAG, "status")

    }

    override fun onProviderDisabled(provider: String?) {
      Log.d(TAG, "pro dis")

    }

    override fun onProviderEnabled(provider: String?) {
      Log.d(TAG, "pro ena")

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

    val location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
    if (location == null) {
      Log.d(TAG, "location null")
      locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 0F, locationListener)
    } else {
      Log.d(TAG, location.latitude.toString() + "," + location.longitude.toString())
      changeActivityWithLocation(location)
    }
  }

  private fun changeActivityWithLocation(location: android.location.Location) {
    doAsync {
      val geo = Geocoder(this@SplashActivity)

      val latitude = location.latitude
      val longitude = location.longitude

      val locationInformation = geo.getFromLocation(latitude, longitude, 10)[0]

      val countryCode = locationInformation.countryCode
      val adminArea = locationInformation.adminArea
      val locality = locationInformation.locality ?: locationInformation.adminArea


      Log.d("test", locationInformation.toString())
      val displayName: String
      if (adminArea == locality) {
        displayName = getString(R.string.channel_area2, locality, countryCode)
      } else {
        displayName = getString(R.string.channel_area1, locality, adminArea, countryCode)
      }

      val name = countryCode.replace(" ", "").toLowerCase() + "-" +
          adminArea.replace(" ", "").toLowerCase() + "-" +
          locality.replace(" ", "").toLowerCase()

      getServer(Server("", name, displayName, countryCode, adminArea, locality))
    }
  }

  private fun getServer(server: Server) {
    val serverNameRef = databaseReference.orderByChild("name").equalTo(server.name)
    serverNameRef.addListenerForSingleValueEvent(object: ValueEventListener {
      override fun onDataChange(p0: DataSnapshot) {
        if (!p0.hasChildren()) {
          val serverRef = databaseReference.push()
          serverRef.setValue(Server(serverRef.key!!, server.name, server.displayName, server.countryCode, server.adminArea, server.locality))
            .addOnCompleteListener {
              val channels = arrayListOf("general", "trade")
              for (channel in channels) {
                val channelRef = FirebaseDatabase.getInstance().getReference("/servers/${serverRef.key}/channels").push()
                channelRef.setValue(Channel(channelRef.key!!, channel))

                if (channel == "general") {

                  val uid = FirebaseAuth.getInstance().uid
                  if (uid != null) {
                    FirebaseDatabase.getInstance().getReference("/users/$uid/currentServer").setValue(serverRef.key)
                    FirebaseDatabase.getInstance().getReference("/users/$uid/currentChannel").setValue(channelRef.key)

                    val favoriteServerRef = FirebaseDatabase.getInstance().getReference("/users/$uid/servers")
                    favoriteServerRef.orderByChild("serverId").equalTo(serverRef.key)
                      .addListenerForSingleValueEvent(object: ValueEventListener {
                        override fun onDataChange(p0: DataSnapshot) {
                          if (!p0.hasChildren()) {
                            val pushFavoriteServerRef = favoriteServerRef.push()
                            pushFavoriteServerRef.setValue(FavoriteServer(pushFavoriteServerRef.key, serverRef.key, server.displayName))
                            favoriteServerRef.removeEventListener(this)
                          }
                        }

                        override fun onCancelled(p0: DatabaseError) {
                        }
                      })
                  }

                  Log.d("test", server.displayName)

                  nextIntent.putExtra(CURRENT_SERVER, FavoriteServer(null, serverRef.key!!, server.displayName))
                  nextIntent.putStringArrayListExtra(CURRENT_CHANNEL, arrayListOf(channelRef.key!!, channel))

                  nextIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)

                  startActivity(nextIntent)
                  finish()
                  serverNameRef.removeEventListener(this)
                }
              }
            }
        } else {
          p0.children.forEach {
            val serverInfo = it.getValue(Server::class.java) ?: return
            val channelRef = FirebaseDatabase.getInstance().getReference("/servers/${serverInfo.id}/channels").limitToFirst(1)
            channelRef.addListenerForSingleValueEvent(object: ValueEventListener {
              override fun onDataChange(p0: DataSnapshot) {
                Log.d("test", "in channel")

                val channel = p0.children.first().getValue(Channel::class.java) ?: return

                val uid = FirebaseAuth.getInstance().uid
                if (uid != null) {
                  FirebaseDatabase.getInstance().getReference("/users/$uid/currentServer").setValue(serverInfo.id)
                  FirebaseDatabase.getInstance().getReference("/users/$uid/currentChannel").setValue(channel.id)

                  val favoriteServerRef = FirebaseDatabase.getInstance().getReference("/users/$uid/servers")
                  favoriteServerRef.orderByChild("serverId").equalTo(serverInfo.id)
                    .addListenerForSingleValueEvent(object: ValueEventListener {
                      override fun onDataChange(p0: DataSnapshot) {
                        if (!p0.hasChildren()) {
                          val pushFavoriteServerRef = favoriteServerRef.push()
                          pushFavoriteServerRef.setValue(FavoriteServer(pushFavoriteServerRef.key, serverInfo.id, serverInfo.displayName))
                          favoriteServerRef.removeEventListener(this)
                        }
                      }

                      override fun onCancelled(p0: DatabaseError) {
                      }
                    })
                }

                nextIntent.putExtra(CURRENT_SERVER, FavoriteServer(null, serverInfo.id, server.displayName))
                nextIntent.putStringArrayListExtra(CURRENT_CHANNEL, arrayListOf(channel.id, channel.name))

                nextIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(nextIntent)
                finish()
                channelRef.removeEventListener(this)
              }

              override fun onCancelled(p0: DatabaseError) {
              }
            })
          }
        }
      }

      override fun onCancelled(p0: DatabaseError) {
      }
    })
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
