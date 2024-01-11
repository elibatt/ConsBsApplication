package com.unibs.consbs

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.unibs.consbs.databinding.LoginBinding
import com.unibs.consbs.profile.ProfiloActivity
import com.unibs.consbs.utils.Utils

class LoginActivity: AppCompatActivity() {

    lateinit var binding: LoginBinding
    lateinit var mAuth: FirebaseAuth
    lateinit var sharedPreferences: SharedPreferences
    lateinit var db: DBHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        mAuth = Firebase.auth

        db = DBHelper()

        super.onCreate(savedInstanceState)
        binding = LoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Utils.setCurrentActivty("LoginActivity")

        val currentUser = mAuth.currentUser

        if (currentUser != null) {
            if ( currentUser!!.uid != null ) {
                val intent = Intent(this@LoginActivity, ProfiloActivity::class.java)

                startActivity(intent)
                finish()
            }
        }

        binding.apply {

            emailutente.addTextChangedListener(object: TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    emailutente.backgroundTintList = getColorStateList(R.color.gray_edittext)
                    emailutente.setTextColor(getResources().getColor(R.color.black))

                    passwordutente.backgroundTintList = getColorStateList(R.color.gray_edittext)
                    passwordutente.setTextColor(getResources().getColor(R.color.black))

                    messaggioErrore.text = ""
                }

                override fun afterTextChanged(s: Editable?) {

                }

            })

            passwordutente.addTextChangedListener(object: TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    emailutente.backgroundTintList = getColorStateList(R.color.gray_edittext)
                    emailutente.setTextColor(getResources().getColor(R.color.black))

                    passwordutente.backgroundTintList = getColorStateList(R.color.gray_edittext)
                    passwordutente.setTextColor(getResources().getColor(R.color.black))

                    messaggioErrore.text = ""
                }

                override fun afterTextChanged(s: Editable?) {

                }

            })

            submitutente.setOnClickListener{
                var email = emailutente.text.toString().trim()
                var password = passwordutente.text.toString().trim()

                if ( email.isEmpty() || !Utils.isEmailValid(email) ) {
                    emailutente.backgroundTintList = getColorStateList(R.color.red_palette)
                    emailutente.setTextColor(getResources().getColor(R.color.red_palette))
                    messaggioErrore.text = "L'email inserita non è valida!"
                } else if ( password.isEmpty() ) {
                    passwordutente.backgroundTintList = getColorStateList(R.color.red_palette)
                    passwordutente.setTextColor(getResources().getColor(R.color.red_palette))
                    messaggioErrore.text = "Il campo \"password\" è da compilare"
                } else {
                    login(email, password)
                }
            }

        }
    }

    private fun login(email: String, password: String) {
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val intent = Intent(this@LoginActivity, ProfiloActivity::class.java)

                    val user: FirebaseUser? = mAuth.currentUser

                    sharedPreferences = getSharedPreferences("preferences", Context.MODE_PRIVATE)
                    var editor = sharedPreferences.edit()
                    val isStudente = email.contains("@stud.consbs.it")
                    editor.putString("uid", user!!.uid)
                    editor.putBoolean("isStudente", isStudente)

                    FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                        if (!task.isSuccessful) {
                            Log.w(ContentValues.TAG, "Fetching del token fallito", task.exception)
                            return@OnCompleteListener
                        }

                        // Get new FCM registration token
                        val token = task.result
                        sharedPreferences = getSharedPreferences("preferences", Context.MODE_PRIVATE)
                        editor = sharedPreferences.edit()
                        editor.putString("my_id_device", token)
                        editor.apply()

                        db.aggiungi_device(user!!.uid, token)
                    })

                    if ( isStudente ) {
                        db.associa_studente_a_materie(user!!.uid)
                    }

                    Toast.makeText(this@LoginActivity, "Autenticazione avvenuta correttamente!", Toast.LENGTH_LONG).show()

                    editor.apply()
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, "Autenticazione Fallita!", Toast.LENGTH_LONG).show()

                    binding.emailutente.backgroundTintList = getColorStateList(R.color.red_palette)
                    binding.emailutente.setTextColor(getResources().getColor(R.color.red_palette))

                    binding.passwordutente.backgroundTintList = getColorStateList(R.color.red_palette)
                    binding.passwordutente.setTextColor(getResources().getColor(R.color.red_palette))

                    binding.messaggioErrore.text = "Autenticazione Fallita!"
                }
            }
    }

}