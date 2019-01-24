package com.gtgalone.gulliver

import android.content.Intent
import android.os.Bundle
import android.view.*
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.gtgalone.gulliver.models.User
import kotlinx.android.synthetic.main.fragment_people_bottom_sheet_dialog.view.*

class PeopleBottomSheetDialogFragment : BottomSheetDialogFragment() {
  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

    return inflater.inflate(R.layout.fragment_people_bottom_sheet_dialog, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val user = arguments!!.getParcelable<User>(MainActivity.USER_KEY) ?: return

    view.people_bottom_sheet_dialog_fragment_display_name.text = user.displayName
    Glide.with(view).load(user.photoUrl).into(view.people_bottom_sheet_dialog_fragment_photo)
    view.people_bottom_sheet_dialog_fragment_direct_message.setOnClickListener {
      val intent = Intent(view.context, DirectMessagesLogActivity::class.java)
      intent.putExtra(MainActivity.USER_KEY, user)
      startActivity(intent)
    }
  }
}