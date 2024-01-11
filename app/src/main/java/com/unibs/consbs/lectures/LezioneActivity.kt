package com.unibs.consbs.lectures

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
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
import com.unibs.consbs.Professore_Materia
import com.unibs.consbs.Studente_Materia
import com.unibs.consbs.bookings.PrenotazioniAdapter
import com.unibs.consbs.chat.MessaggiActivity
import com.unibs.consbs.data.Evento
import com.unibs.consbs.data.Materia
import com.unibs.consbs.databinding.LezioniBinding
import com.unibs.consbs.databinding.PrenotazioniBinding
import com.unibs.consbs.utils.FirebaseUtils
import com.unibs.consbs.utils.Utils
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class LezioneActivity: BaseActivity() {
    lateinit var binding: LezioniBinding
    private lateinit var lezioniRecyclerView: RecyclerView
    private lateinit var lezioniList: ArrayList<Evento>
    private lateinit var idMaterieList: ArrayList<String>
    private lateinit var adapter: LezioniAdapter
    private lateinit var mDbRef: DatabaseReference


    override fun onCreate(savedInstanceState: Bundle?) {

        mAuth = Firebase.auth
        val currentUser = mAuth.currentUser

        if ( intent.getStringExtra("action") == "OPEN_ACTIVITY" ) {
            val id_device = intent.getStringExtra("id_device")
            val nominativo = intent.getStringExtra("nominativo")
            val id_chat = intent.getStringExtra("id_chat")
            val id_studente_partecipante = intent.getStringExtra("id_studente_partecipante")

            val intent = Intent(this, MessaggiActivity::class.java)
            intent.putExtra("id_device", id_device)
            intent.putExtra("nominativo", nominativo)
            intent.putExtra("id_chat", id_chat)
            intent.putExtra("id_studente_partecipante", id_studente_partecipante)
            startActivity(intent)
            finish()
        }

        super.onCreate(savedInstanceState)
        binding = LezioniBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Utils.setCurrentActivty("LezioniActivity")

        FirebaseUtils.checkUserLoggedIn(this)

        binding.menu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        // Menù
        setNavigationViewSelectedItem("Lezioni")
        setOptionsItemsListener()

        // preferenze di sessione
        sharedPreferences = getSharedPreferences("preferences", Context.MODE_PRIVATE)
        val isStudente = sharedPreferences.getBoolean("isStudente", false)



        binding.swiperefresh.setOnRefreshListener {
            setOnRefreshLayer()
        }

        lezioniList = ArrayList()
        idMaterieList = ArrayList()
        sharedPreferences = getSharedPreferences("preferences", Context.MODE_PRIVATE)

        adapter = LezioniAdapter(this, lezioniList)
        lezioniRecyclerView = binding.lezioniRecyclerView
        lezioniRecyclerView.layoutManager = LinearLayoutManager(this)
        lezioniRecyclerView.adapter = adapter
        mDbRef = Firebase.database.reference

        val materieRef = mDbRef.child("studenti_materie").orderByChild("id_studente").equalTo(currentUser?.uid.toString())
        if ( !isStudente ) {
            val materieProfRef = mDbRef.child("professori_materie").orderByChild("id_professore").equalTo(currentUser?.uid.toString())
            materieProfRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    idMaterieList.clear()
                    for (dataSnapshot in snapshot.children) {
                        val professore_materia = dataSnapshot.getValue(Professore_Materia::class.java)
                        idMaterieList.add(professore_materia?.id_materia.toString())


                    }
                    val editor = sharedPreferences.edit()
                    val serializedArray = idMaterieList.joinToString(separator = ",")
                    //io ho materie associate con id 14 e 3 e stampando ogni elemento dell'array esce 1 4 , 3
                    for(elementoArray in serializedArray){
                        Log.d("elemento Array", elementoArray.toString())
                    }
                    editor.putString("idMaterieList", serializedArray)
                    editor.apply()

                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("Errore ciclo eventi", "Ciclo materie")
                }

            })
        }else {
            materieRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    idMaterieList.clear()
                    for (dataSnapshot in snapshot.children) {
                        val studente_materia = dataSnapshot.getValue(Studente_Materia::class.java)
                        idMaterieList.add(studente_materia?.id_materia.toString())

                        val editor = sharedPreferences.edit()
                        val serializedArray = idMaterieList.joinToString(separator = ",")
                        editor.putString("idMaterieList", serializedArray)
                        editor.apply()
                    }

                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("Errore ciclo eventi", "Ciclo materie")
                }

            })
        }

        var c = Calendar.getInstance()
        if ( c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY ) c.add(Calendar.DAY_OF_YEAR, 1)
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        binding.dataPrenotazione.text="$day/${month + 1}/$year"
        binding.dataPrenotazione.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                this,
                { view, year, monthOfYear, dayOfMonth ->
                    val selectedDate = Calendar.getInstance()
                    selectedDate.set(year, monthOfYear, dayOfMonth)
                    val dayOfWeek = selectedDate.get(Calendar.DAY_OF_WEEK)
                    if(dayOfWeek!= Calendar.SUNDAY) {
                        binding.dataPrenotazione.setText(dayOfMonth.toString() + "/" + (monthOfYear + 1) + "/" + year)

                        get_eventi(binding.dataPrenotazione.text.toString())
                    }else{
                        Toast.makeText(this@LezioneActivity, "Attenzione, Domenica il Conservatorio è chiuso",
                            Toast.LENGTH_SHORT).show()
                    }
                },

                year,
                month,
                day,

                )
            //datePickerDialog.datePicker.minDate=System.currentTimeMillis()
            //datePickerDialog.datePicker.maxDate=System.currentTimeMillis()+72*60*60*1000
            datePickerDialog.show()
        }

        binding.indietroGiorno.setOnClickListener{
            c.add(Calendar.DAY_OF_YEAR, -1)

            if ( c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY ) c.add(Calendar.DAY_OF_YEAR, -1)

            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            binding.dataPrenotazione.text="$day/${month + 1}/$year"

            get_eventi(binding.dataPrenotazione.text.toString())
        }

        binding.avantiGiorno.setOnClickListener{
            c.add(Calendar.DAY_OF_YEAR, +1)

            if ( c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY ) c.add(Calendar.DAY_OF_YEAR, +1)

            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            binding.dataPrenotazione.text="$day/${month + 1}/$year"

            get_eventi(binding.dataPrenotazione.text.toString())
        }

        get_eventi(binding.dataPrenotazione.text.toString())

    }

    private fun get_eventi(data_prenotazione: String) {

        val dateFormat = SimpleDateFormat("dd/MM/yyyy")

        val eventiRef = mDbRef.child("eventi").orderByChild("id_materia")
        eventiRef.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                lezioniList.clear()
                for(eventoSnapshot in snapshot.children){
                    val evento = eventoSnapshot.getValue(Evento::class.java)
                    if( evento?.prenotazione==false &&
                        idMaterieList.contains(evento?.id_materia.toString()) &&
                        Utils.checkTimestampDay(evento.data_ora_inizio.toLong(), dateFormat.parse(data_prenotazione))
                    ) {
                        lezioniList.add(evento)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("Errore ciclo eventi","Ciclo lezioni")
            }

        } )
    }
}