package com.unibs.consbs.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import com.unibs.consbs.LoginActivity

object FirebaseUtils {
    fun checkUserLoggedIn(context: Context) {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser == null) {
            // L'utente non è loggato, reindirizza alla LoginActivity
            val intent = Intent(context, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)

            if (context is Activity) {
                context.finish()
            }
        }
    }
}
