package com.unibs.consbs.bookings

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.unibs.consbs.BaseActivity
import com.unibs.consbs.Prenotazione
import com.unibs.consbs.classrooms.AuleAdapter
import com.unibs.consbs.data.Aula
import com.unibs.consbs.data.Evento
import com.unibs.consbs.databinding.AuleBinding
import com.unibs.consbs.databinding.PrenotazioniBinding
import com.unibs.consbs.utils.FirebaseUtils
import com.unibs.consbs.utils.Utils

class PrenotazioniActivity : BaseActivity() {
    lateinit var binding: PrenotazioniBinding
    private lateinit var prenotazioniRecyclerView: RecyclerView
    private lateinit var prenotazioniList: ArrayList<Evento>
    private lateinit var adapter: PrenotazioniAdapter
    private lateinit var mDbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {

        mAuth = Firebase.auth
        val currentUser = mAuth.currentUser

        super.onCreate(savedInstanceState)
        binding = PrenotazioniBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Utils.setCurrentActivty("PrenotazioniActivity")

        FirebaseUtils.checkUserLoggedIn(this)

        // preferenze di sessione
        sharedPreferences = getSharedPreferences("preferences", Context.MODE_PRIVATE)
        val isStudente = sharedPreferences.getBoolean("isStudente", false)
        if(isStudente){
            binding.linearSottotitolo.visibility= View.GONE
        }

        binding.menu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        // Menù
        setNavigationViewSelectedItem("Prenotazioni")
        setOptionsItemsListener()

        binding.swiperefresh.setOnRefreshListener {
            setOnRefreshLayer()
        }

        prenotazioniList = ArrayList()
        adapter = PrenotazioniAdapter(this, prenotazioniList)

        prenotazioniRecyclerView = binding.prenotazioniRecyclerView
        prenotazioniRecyclerView.layoutManager = LinearLayoutManager(this)
        prenotazioniRecyclerView.adapter = adapter
        mDbRef = Firebase.database.reference

        val eventiRef = mDbRef.child("eventi").orderByChild("id_user_inserimento").equalTo(currentUser?.uid.toString())
        eventiRef.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                prenotazioniList.clear()
                for(eventoSnapshot in snapshot.children.reversed()){
                    val evento = eventoSnapshot.getValue(Evento::class.java)
                    if( evento?.ricorrente==false  && Utils.isTimestampTodayOrAfter(evento.data_ora_inizio.toLong()) ) prenotazioniList.add(evento)
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("Errore ciclo eventi","Ciclo prenotazioni")
            }

        } )

    }
}