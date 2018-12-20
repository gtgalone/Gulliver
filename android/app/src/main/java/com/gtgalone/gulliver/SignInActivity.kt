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
import com.gtgalone.gulliver.models.FavoriteServer
import kotlinx.android.synthetic.main.activity_sign_in.*

class SignInActivity : AppCompatActivity() {
  private lateinit var mAuth: FirebaseAuth
  private lateinit var mGoogleSignInClient: GoogleSignInClient

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

    sign_in_google_button.setOnClickListener {
      signIn()
    }
  }

  private fun signIn() {
    val signInIntent = mGoogleSignInClient.signInIntent
    startActivityForResult(signInIntent, RC_SIGN_IN)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    Log.d(TAG, requestCode.toString())
    if (requestCode == RC_SIGN_IN) {
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
    val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")

    FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener {
      val token = it.result?.token ?: ""

      val currentServer = intent.getParcelableExtra<FavoriteServer>(SplashActivity.CURRENT_SERVER)
      val currentChannel = intent.getStringArrayListExtra(SplashActivity.CURRENT_CHANNEL)
      ref.limitToFirst(1)
        .addListenerForSingleValueEvent(object: ValueEventListener {
          override fun onDataChange(p0: DataSnapshot) {

            if (p0.hasChildren()) {
                val tokenRef = FirebaseDatabase.getInstance().getReference("/users/$uid/notificationTokens/$token")
                tokenRef.setValue(true)

                val favoriteServerRef = FirebaseDatabase.getInstance().getReference("/users/$uid/servers")
                favoriteServerRef.orderByChild("serverId").equalTo(currentServer.serverId)
                  .addListenerForSingleValueEvent(object: ValueEventListener {
                    override fun onDataChange(p0: DataSnapshot) {
                      if (!p0.hasChildren()) {
                        val pushFavoriteServerRef = favoriteServerRef.push()
                        pushFavoriteServerRef.setValue(FavoriteServer(pushFavoriteServerRef.key, currentServer.serverId, currentServer.serverDisplayName))
                        favoriteServerRef.removeEventListener(this)
                      }
                    }

                    override fun onCancelled(p0: DatabaseError) {
                    }
                  })
            } else {
              ref.setValue(User(uid, displayName, email, photoUrl, currentServer.serverId!!, currentChannel[0])).addOnCompleteListener {
                val tokenRef = FirebaseDatabase.getInstance().getReference("/users/$uid/notificationTokens/$token")
                tokenRef.setValue(true)

                val favoriteServerRef = FirebaseDatabase.getInstance().getReference("/users/$uid/servers/").push()
                Log.d("test", currentServer.serverId + currentServer.serverDisplayName)

                favoriteServerRef.setValue(FavoriteServer(favoriteServerRef.key!!, currentServer.serverId, currentServer.serverDisplayName))

                val nextIntent = Intent(this@SignInActivity, MainActivity::class.java)
                nextIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)

                nextIntent.putExtra(
                  SplashActivity.CURRENT_SERVER,
                  intent.getParcelableExtra<FavoriteServer>(SplashActivity.CURRENT_SERVER)
                )

                nextIntent.putExtra(
                  SplashActivity.CURRENT_CHANNEL,
                  intent.getStringArrayListExtra(SplashActivity.CURRENT_CHANNEL)
                )

                startActivity(nextIntent)
              }
            }
          }
          override fun onCancelled(p0: DatabaseError) {
          }
        })
    }
  }
}
