package com.gtgalone.gulliver

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.gtgalone.gulliver.models.User
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.iid.FirebaseInstanceId
import com.gtgalone.gulliver.models.MyCity
import com.gtgalone.gulliver.models.City
import kotlinx.android.synthetic.main.activity_sign_in.*

class SignInActivity : AppCompatActivity() {
  private lateinit var mGoogleSignInClient: GoogleSignInClient
  private lateinit var mAuth: FirebaseAuth
  private lateinit var nextIntent: Intent

  companion object {
    const val RC_SIGN_IN = 0
    const val TAG = "SignInActivity"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    Log.d("test", "on create sign")
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_sign_in)

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
      .requestIdToken(getString(R.string.default_web_client_id))
      .requestEmail()
      .build()

    mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
    mAuth = FirebaseAuth.getInstance()
    nextIntent = Intent(this@SignInActivity, MainActivity::class.java)

    sign_in_google_button.setOnClickListener {
      signIn()
    }
  }

  private fun signIn() {
    val signInIntent = mGoogleSignInClient.signInIntent
    startActivityForResult(signInIntent, RC_SIGN_IN)
  }

  private fun changeAcitivity() {
    nextIntent.putExtra(
      SplashActivity.CURRENT_CITY,
      intent.getParcelableExtra<MyCity>(SplashActivity.CURRENT_CITY)
    )

    startActivity(nextIntent)
    finish()
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    Log.d(TAG, requestCode.toString())
    if (requestCode == RC_SIGN_IN) {
//      error
//      java.lang.RuntimeException: Failure delivering result ResultInfo{who=null, request=0, result=0, data=Intent { (has extras) }} to activity {com.gtgalone.gulliver/com.gtgalone.gulliver.SignInActivity}: com.google.android.gms.tasks.RuntimeExecutionException: com.google.android.gms.common.api.ApiException: 16:
//      at android.app.ActivityThread.deliverResults(ActivityThread.java:4058)

//      2 lines
//      val task = GoogleSignIn.getSignedInAccountFromIntent(data)
//      val account = task.result!!

      val task = GoogleSignIn.getSignedInAccountFromIntent(data)
      try {
        val account = task.result!!
        firebaseAuthWithGoogle(account)
      } catch (e: ApiException) {
        Log.w(TAG, "Google sign in failed", e)
      }
    }
  }

  private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
    Log.d(TAG, "firebaseAuthWithGoogle:" + acct.id)

    val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
    mAuth.signInWithCredential(credential)
      .addOnCompleteListener {
        if (it.isSuccessful) {
          Log.d(TAG, "signInWithCredential:success")
          val user = mAuth.currentUser!!

          saveUserToFirebaseDatabase(user.displayName!!, user.email!!, user.photoUrl.toString())
        } else {
          Log.d(TAG, "signInWithCredential:failure", it.exception)
          Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show()
        }
      }
  }

  private fun saveUserToFirebaseDatabase(displayName: String, email: String, photoUrl: String) {
    val uid = FirebaseAuth.getInstance().uid ?: ""
    val userRef = FirebaseDatabase.getInstance().getReference("/users/$uid")
    val myCityRef = userRef.child("cities")

    FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener {
      val token = it.result?.token ?: ""

      val currentCity = intent.getParcelableExtra<MyCity>(SplashActivity.CURRENT_CITY)

      val user = User(uid, displayName, email, photoUrl, currentCity.id, "general")

      userRef.limitToFirst(1)
        .addListenerForSingleValueEvent(object: ValueEventListener {
          override fun onDataChange(p0: DataSnapshot) {

            if (p0.hasChildren()) {
              val tokenRef = FirebaseDatabase.getInstance().getReference("/users/$uid/notificationTokens/$token")
              tokenRef.setValue(true)

              myCityRef.orderByChild("id").equalTo(currentCity.id)
                .addListenerForSingleValueEvent(object: ValueEventListener {
                  override fun onDataChange(p0: DataSnapshot) {
                    if (!p0.hasChildren()) {
                      myCityRef.child(currentCity.id).setValue(MyCity(
                        currentCity.id,
                        currentCity.countryCode,
                        currentCity.adminArea,
                        currentCity.locality,
                        currentCity.timestamp
                      ))
                    }
                    changeAcitivity()
                  }
                  override fun onCancelled(p0: DatabaseError) {}
                })
            } else {
              userRef.setValue(user)
                .addOnCompleteListener {
                  val tokenRef = FirebaseDatabase.getInstance().getReference("/users/$uid/notificationTokens/$token")
                  tokenRef.setValue(true)

                  myCityRef.child(currentCity.id).setValue(MyCity(
                    currentCity.id,
                    currentCity.countryCode,
                    currentCity.adminArea,
                    currentCity.locality,
                    currentCity.timestamp
                  ))

                  changeAcitivity()
              }
            }
          }
          override fun onCancelled(p0: DatabaseError) {}
        })
    }
  }
}
