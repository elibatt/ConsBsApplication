package com.unibs.consbs.classrooms

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.unibs.consbs.DBHelper
import com.unibs.consbs.chat.ChatAdapter
import com.unibs.consbs.data.Aula
import com.unibs.consbs.data.Chat
import com.unibs.consbs.data.Evento
import com.unibs.consbs.data.Sede
import com.unibs.consbs.databinding.AuladettaglioBinding
import com.unibs.consbs.databinding.AuleBinding
import com.unibs.consbs.lectures.InserisciLezioneActivity
import com.unibs.consbs.utils.FirebaseUtils
import com.unibs.consbs.utils.Utils
import kotlin.concurrent.timerTask

class AulaDettaglioActivity: BaseActivity() {

    lateinit var binding: AuladettaglioBinding
    private lateinit var aulaDettaglioRecyclerView: RecyclerView
    private lateinit var eventoList: ArrayList<Evento>
    private lateinit var adapter: AulaDettaglioAdapter
    private lateinit var mDbRef: DatabaseReference
    override fun onCreate(savedInstanceState: Bundle?) {

        mAuth = Firebase.auth
        val currentUser = mAuth.currentUser

        super.onCreate(savedInstanceState)
        binding = AuladettaglioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Utils.setCurrentActivty("AulaDettaglioActivty")

        sharedPreferences = getSharedPreferences("preferences", Context.MODE_PRIVATE)
        val isStudente = sharedPreferences.getBoolean("isStudente", false)

        FirebaseUtils.checkUserLoggedIn(this)

        binding.menu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        // Menù
        setNavigationViewSelectedItem("Aule")
        setOptionsItemsListener()

        binding.swiperefresh.setOnRefreshListener {
            setOnRefreshLayer()
        }

        eventoList = ArrayList()
        adapter = AulaDettaglioAdapter(this, eventoList)
        aulaDettaglioRecyclerView = binding.auleDettaglioRecyclerView
        aulaDettaglioRecyclerView.layoutManager = LinearLayoutManager(this)
        aulaDettaglioRecyclerView.adapter = adapter
        mDbRef = Firebase.database.reference

        val sediHashMap = HashMap<String, String>()
        sediHashMap["sede_bs1"] = "Sede BS 1"
        sediHashMap["sede_bs2"] = "Sede BS 2"
        sediHashMap["sede_dbt"] = "Sede Darfo"

        val pianiHashMap = HashMap<Int, String>()
        pianiHashMap[0] = "Piano Terra"
        pianiHashMap[1] = "1° Piano"
        pianiHashMap[2] = "2° Piano"

        val id_aula = intent.getIntExtra("id_aula",0)
        var indirizzo = ""
        var nome_sede=""
        var nome_aula=""
        var nome_piano=""
        mDbRef.child("aule").child(id_aula.toString()).addValueEventListener(object:ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val aula = snapshot.getValue(Aula::class.java)
                    if (aula != null) {
                        binding.textAulaNome.text = "Aula "+aula.nome
                        nome_aula="Aula "+aula.nome
                        binding.textSede.text = "⋄  "+sediHashMap[aula.id_sede]
                        nome_sede="⋄  "+sediHashMap[aula.id_sede].toString()
                        binding.textPiano.text = "⋄  "+pianiHashMap[aula.piano]
                        nome_piano="⋄  "+pianiHashMap[aula.piano]

                        mDbRef.child("sedi").child(aula.id_sede.toString()).addValueEventListener(object:ValueEventListener{
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (snapshot.exists()) {
                                    val sede = snapshot.getValue(Sede::class.java)
                                    if(sede!=null){
                                        binding.textIndirizzo.text = "⋄  "+sede.indirizzo
                                        indirizzo= "⋄  "+sede.indirizzo
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.d("Errore indirizzo","Errore:$error")
                            }

                        })

                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("GET studenti-chat", "Errore: ${error.message}")
            }

        })


        val eventiRef = mDbRef.child("eventi").orderByChild("id_aula").equalTo(id_aula.toDouble())
        eventiRef.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                eventoList.clear()
                for ( eventoSnapshot in snapshot.children ) {
                    val evento = eventoSnapshot.getValue(Evento::class.java)
                    if(Utils.isTimestampToday(evento!!.data_ora_inizio.toLong()))
                        eventoList.add(evento!!)

                }
                adapter.notifyDataSetChanged()

                val timestampoggi = System.currentTimeMillis().toDouble()

                var uscitaFor = false
                for(event in eventoList){
                    if(event.data_ora_inizio<= timestampoggi && timestampoggi<=event.data_ora_fine){
                        binding.textStatoattuale.text= "Stato attuale: 🔴"
                        uscitaFor= true
                        break
                    }else if(event.data_ora_inizio-30*60*1000.0 <= timestampoggi && timestampoggi<=event.data_ora_fine){
                        binding.textStatoattuale.text= "Stato attuale: 🟠"
                        uscitaFor=true
                        break
                    }
                }
                if(uscitaFor==false) binding.textStatoattuale.text= "Stato attuale: 🟢"
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("GET studenti-chat", "Errore: ${error.message}")
            }

        })

            binding.prenotaAula.setOnClickListener{
                if(isStudente) {
                    val intent = Intent(this, PrenotazioneAulaActivity::class.java)
                    intent.putExtra("indirizzo", indirizzo)
                    intent.putExtra("nome_sede", nome_sede)
                    intent.putExtra("nome_aula", nome_aula)
                    intent.putExtra("nome_piano", nome_piano)
                    intent.putExtra("id_aula", id_aula)
                    startActivity(intent)
                    finish()
                }else{
                    val intent = Intent(this, InserisciLezioneActivity::class.java)
                    intent.putExtra("indirizzo", indirizzo)
                    intent.putExtra("nome_sede", nome_sede)
                    intent.putExtra("nome_aula", nome_aula)
                    intent.putExtra("nome_piano", nome_piano)
                    intent.putExtra("id_aula", id_aula)
                    startActivity(intent)
                    finish()

                }
            }

        }

    }