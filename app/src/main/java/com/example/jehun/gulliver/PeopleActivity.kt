package com.example.jehun.gulliver

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.jehun.gulliver.models.User
import com.example.jehun.gulliver.models.UserItem
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_people.*

class PeopleActivity : AppCompatActivity() {
  companion object {
    const val USER_KEY = "USER_KEY"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_people)

    supportActionBar!!.title = "People"

    fetchUsers()
  }

  private fun fetchUsers() {
    val ref = FirebaseDatabase.getInstance().getReference("/users")

    ref.addListenerForSingleValueEvent(object : ValueEventListener {
      override fun onDataChange(p0: DataSnapshot) {
        val adapter = GroupAdapter<ViewHolder>()
        p0.children.forEach {
          val user = it.getValue(User::class.java)

          if (user != null) adapter.add(UserItem(user))
          adapter.setOnItemClickListener { item, view ->

            val userItem = item as UserItem

            val intent = Intent(view.context, DirectMessageLogActivity::class.java)
            intent.putExtra(USER_KEY, userItem.user)
            startActivity(intent)
          }
        }
        recycler_view_people.adapter = adapter
      }

      override fun onCancelled(p0: DatabaseError) {
      }
    })

  }

}
