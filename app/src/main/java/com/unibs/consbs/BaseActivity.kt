package com.unibs.consbs

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEach
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.unibs.consbs.bookings.PrenotazioniActivity
import com.unibs.consbs.chat.ChatActivity
import com.unibs.consbs.classrooms.AuleActivity
import com.unibs.consbs.lectures.InserisciLezioneActivity
import com.unibs.consbs.lectures.LezioneActivity
import com.unibs.consbs.profile.ProfiloActivity

open class BaseActivity: AppCompatActivity() {

    lateinit var mAuth: FirebaseAuth
    lateinit var navigationView: NavigationView
    lateinit var sharedPreferences : SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_menu)

        navigationView = findViewById(R.id.navView)

        // navigationView.setNavigationItemSelectedListener { menuItem -> optionsItemSelected(menuItem) }

        val chatMenuItem = navigationView.menu.findItem(R.id.menu_chat)
        Log.d("chatMenuItem", chatMenuItem.toString())
        chatMenuItem.setOnMenuItemClickListener {
            Log.d("onCLick chat", "sonoq")
            val intent = Intent(this, ChatActivity::class.java)
            startActivity(intent)
            finish()
            true
        }
    }

    protected fun setNavigationViewSelectedItem(itemTitle: String) {
        val navView = findViewById<NavigationView>(R.id.navView)
        navView.menu.forEach { menuItem ->
            menuItem.isChecked = menuItem.title == itemTitle
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    protected fun setOptionsItemsListener(): Boolean {

        sharedPreferences = getSharedPreferences("preferences", Context.MODE_PRIVATE)
        val isStudente = sharedPreferences.getBoolean("isStudente", false)

        navigationView = findViewById(R.id.navView)

        if ( !isStudente ) {
            Log.d("check-studente","non sono studente")
            val chatMenuItem = navigationView.menu.findItem(R.id.menu_chat)
            chatMenuItem.title = "Inserisci Lezione"
        }

        navigationView.setNavigationItemSelectedListener { menuItem -> optionsItemSelected(menuItem) }
        return true
    }

    private fun optionsItemSelected(menuItem: MenuItem): Boolean {

        sharedPreferences = getSharedPreferences("preferences", Context.MODE_PRIVATE)
        val isStudente = sharedPreferences.getBoolean("isStudente", false)

        when(menuItem.itemId) {
            R.id.menu_lezioni -> {
                val intent = Intent(this, LezioneActivity::class.java)
                startActivity(intent)
                finish()
                return true
            }
            R.id.menu_aule -> {
                val intent = Intent(this, AuleActivity::class.java)
                startActivity(intent)
                finish()
                return true
            }
            R.id.menu_prenotazioni -> {
                val intent = Intent(this, PrenotazioniActivity::class.java)
                startActivity(intent)
                finish()
                return true
            }
            R.id.menu_chat -> {
                var intent = Intent(this, ChatActivity::class.java)
                if ( !isStudente ) {
                    intent = Intent(this, InserisciLezioneActivity::class.java)
                }

                startActivity(intent)
                finish()
                return true
            }
            R.id.menu_profilo -> {
                val intent = Intent(this, ProfiloActivity::class.java)
                startActivity(intent)
                finish()
                return true
            }
            R.id.menu_logout -> {
                mAuth = Firebase.auth
                mAuth.signOut()
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
                return true
            }
            else -> return false
        }
    }

    protected fun setOnRefreshLayer(){
        finish()
        overridePendingTransition(0, 0)
        startActivity(intent)
        overridePendingTransition(0, 0)
        //Il metodo recreate è buggato su Xiaomi MIUI
        //recreate()
    }

}