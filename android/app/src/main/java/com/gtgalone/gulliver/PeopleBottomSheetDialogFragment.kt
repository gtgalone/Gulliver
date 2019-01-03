package com.gtgalone.gulliver

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.gtgalone.gulliver.models.User
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.people_bottom_sheet_dialog_fragment.view.*

class PeopleBottomSheetDialogFragment : BottomSheetDialogFragment() {
  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

    return inflater.inflate(R.layout.people_bottom_sheet_dialog_fragment, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val user = arguments!!.getParcelable<User>(MainActivity.USER_KEY) ?: return
    Log.d("test", user.displayName)

    view.people_bottom_sheet_dialog_fragment_display_name.text = user.displayName
    Picasso.get().load(user.photoUrl).fit().centerCrop().into(view.people_bottom_sheet_dialog_fragment_photo)
    view.people_bottom_sheet_dialog_fragment_direct_message.setOnClickListener {
      val intent = Intent(view.context, DirectMessagesLogActivity::class.java)
      intent.putExtra(MainActivity.USER_KEY, user)
      startActivity(intent)
    }
  }
}