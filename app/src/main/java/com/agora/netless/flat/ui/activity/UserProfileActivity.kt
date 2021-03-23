package com.agora.netless.flat.ui.activities

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.agora.netless.flat.R
import com.agora.netless.flat.ui.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UserProfileActivity : AppCompatActivity() {

    private val viewModel by viewModels<UserViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        findViewById<TextView>(R.id.fetchInfo).setOnClickListener {
            viewModel.getUsers().observe(
                this, Observer {
                    if (it.data != null) {
                        findViewById<TextView>(R.id.name).text = it.data.name
                        findViewById<TextView>(R.id.sex).text = it.data.sex
                    } else {
                        Log.e("Agora", "" + it.message)
                    }
                }
            )
        }
    }
}