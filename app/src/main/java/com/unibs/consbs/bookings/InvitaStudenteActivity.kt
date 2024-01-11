package com.unibs.consbs.bookings

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.unibs.consbs.data.Studente
import com.unibs.consbs.databinding.InvitaStudenteBinding
import com.unibs.consbs.utils.FirebaseUtils
import com.unibs.consbs.utils.Utils

class InvitaStudenteActivity: AppCompatActivity() {

    lateinit var binding: InvitaStudenteBinding
    private lateinit var mDbRef: DatabaseReference
    private lateinit var mAuth: FirebaseAuth
    private lateinit var invitaStudenteRecyclerView: RecyclerView
    private lateinit var studentiList: ArrayList<Studente>
    private lateinit var adapter: InvitaStudenteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {

        mAuth = Firebase.auth

        super.onCreate(savedInstanceState)
        binding = InvitaStudenteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Utils.setCurrentActivty("InvitaStudenteActivity")

        FirebaseUtils.checkUserLoggedIn(this)

        //settiamo il popup con displaymetrics
        val displaymetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displaymetrics)
        val width= displaymetrics.widthPixels
        val height= displaymetrics.heightPixels
        window.setLayout((width*.8).toInt(),(height*.7).toInt())
        var params: WindowManager.LayoutParams = window.attributes
        params.gravity= Gravity.CENTER
        params.x=0
        params.y=-20
        window.attributes=params

        val id_evento = intent.getStringExtra("id_evento")
        val messaggio =intent.getStringExtra("messaggio")
        studentiList = ArrayList()
        adapter = InvitaStudenteAdapter(this, studentiList, id_evento,messaggio)
        invitaStudenteRecyclerView = binding.studentiRecyclerView
        invitaStudenteRecyclerView.layoutManager = LinearLayoutManager(this)
        invitaStudenteRecyclerView.adapter = adapter
        mDbRef = Firebase.database.reference

        var testoRicercaStudente = ""
        val cercaStudente : EditText = binding.cercaStudente
        cercaStudente.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                testoRicercaStudente = s.toString().lowercase().trim()

                search_studenti(testoRicercaStudente)
            }

            override fun afterTextChanged(s: Editable?) {

            }

        })
    }

    private fun search_studenti(testo: String) {

        mDbRef.child("studenti").addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                studentiList.clear()
                for ( studenteSnapshot in snapshot.children ) {
                    val studente = studenteSnapshot.getValue(Studente::class.java)
                    if ( ( studente?.email.toString().contains(testo) ||
                         studente?.nome?.lowercase().toString().contains(testo) ||
                         studente?.cognome?.lowercase().toString().contains(testo) ) &&
                        studente?.id != mAuth.currentUser?.uid ) {
                            studentiList.add(studente!!)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("GET studenti", "Errore: ${error.message}")
            }

        })
    }

}