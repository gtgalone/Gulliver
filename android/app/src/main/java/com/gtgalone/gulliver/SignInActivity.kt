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
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_sign_in.*

class SignInActivity : AppCompatActivity() {
  private lateinit var mAuth: FirebaseAuth
  private lateinit var mGoogleSignInClient: GoogleSignInClient

  companion object {
    const val RC_SIGN_IN = 0
    const val TAG = "SignInActivity"
  }

  override fun onCreate(savedInstanceState: Bundle?) {

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
          Log.d(TAG, user.displayName)
          Log.d(TAG, user.email)
          Log.d(TAG, user.photoUrl.toString())
          Log.d(TAG, user.providerId)

          saveUserToFirebaseDatabase(user.displayName!!, user.email!!, user.photoUrl.toString())

          val intent = Intent(this, MainActivity::class.java)
          intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
          startActivity(intent)
        } else {
          Log.d(TAG, "signInWithCredential:failure", it.exception)
          Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show()
        }
      }
  }

  private fun saveUserToFirebaseDatabase(displayName: String, email: String, photoUrl: String) {
    val uid = FirebaseAuth.getInstance().uid ?: ""
    val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")
    ref.setValue(User(uid, displayName, email, photoUrl))
  }
}
