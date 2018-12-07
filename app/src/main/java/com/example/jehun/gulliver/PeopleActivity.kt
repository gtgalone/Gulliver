package com.example.jehun.gulliver

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_people.*
import kotlinx.android.synthetic.main.activity_people_item.view.*

class PeopleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_people)

        supportActionBar!!.title = "People"

        fetchUsers()
    }

    private fun fetchUsers() {
        val ref = FirebaseDatabase.getInstance().getReference("/users")

        ref.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {
                val adapter = GroupAdapter<ViewHolder>()
                p0.children.forEach {
                    val user = it.getValue(User::class.java)

                    if (user != null) adapter.add(UserItem(user))
                }
                recyclerview_people.adapter = adapter
            }

            override fun onCancelled(p0: DatabaseError) {
            }
        })

    }

}

class UserItem(val user: User): Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.people_item_display_name_text_view.text = user.displayName
        Picasso.get().load(user.photoUrl).into(viewHolder.itemView.people_item_photo_image_view)
    }

    override fun getLayout(): Int {
        return R.layout.activity_people_item
    }
}