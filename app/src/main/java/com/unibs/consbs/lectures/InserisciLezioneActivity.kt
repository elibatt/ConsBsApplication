package com.unibs.consbs.lectures

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RadioButton
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.unibs.consbs.BaseActivity
import com.unibs.consbs.DBHelper
import com.unibs.consbs.R
import com.unibs.consbs.classrooms.PopupActivity
import com.unibs.consbs.data.Evento
import com.unibs.consbs.databinding.InserisciLezioneBinding
import com.unibs.consbs.profile.ModificaProfiloActivity
import com.unibs.consbs.utils.FirebaseUtils
import com.unibs.consbs.utils.Utils
import java.text.SimpleDateFormat
import java.util.HashMap

class InserisciLezioneActivity: BaseActivity() {

    lateinit var binding: InserisciLezioneBinding
    private lateinit var db: DBHelper
    var isIndividualeVisible : Boolean= false
    lateinit var mDbRef: DatabaseReference
    lateinit var bundle: Bundle



    override fun onCreate(savedInstanceState: Bundle?) {

        mAuth = Firebase.auth
        val currentUser = mAuth.currentUser

        super.onCreate(savedInstanceState)

        binding = InserisciLezioneBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Utils.setCurrentActivty("InserisciLezioneActivity")

        FirebaseUtils.checkUserLoggedIn(this)

        binding.menu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        // preferenze di sessione
        sharedPreferences = getSharedPreferences("preferences", Context.MODE_PRIVATE)
        val isStudente = sharedPreferences.getBoolean("isStudente", false)

        // Menù
        setNavigationViewSelectedItem("Chat")
        setOptionsItemsListener()

        //la penso a partire da prenotazione aula, visto che il professore tendenzialmente sa l'aula che vuole
        val nome_aula=intent.getStringExtra("nome_aula")
        val nome_sede=intent.getStringExtra("nome_sede")
        val nome_piano=intent.getStringExtra("nome_piano")
        val indirizzo=intent.getStringExtra("indirizzo")
        val id_aula=intent.getIntExtra("id_aula",0)
        if(nome_aula!=null) {
            binding.textAulaNome.text = nome_aula
            binding.textSede.text = nome_sede
            binding.textIndirizzo.text = indirizzo
            binding.textPiano.text = nome_piano
        }else{
            //Altrimenti arrivo da inserisci prenotazione e non so l'aula che voglio, ecco perchè io aggiungo ai framment l'aula
            //YOLO (ps. hai fatto bene)
            binding.linarNomeAula.visibility=View.GONE
            binding.linearInfoAula.visibility=View.GONE
        }

        val inserisciLezioneIndividuale = InserisciIndividualeFragment()
        val inserisciLezioneRicorrente = InserisciRicorrenteFragment()
        var fragment:Fragment
        binding.btnFragment.setOnClickListener{
            if(!isIndividualeVisible){
                //makeCurrentFragment(inserisciLezioneIndividuale)
                fragment= inserisciLezioneIndividuale
                isIndividualeVisible=true
                binding.btnFragment.text="Passa a lezione ricorrente"
            }else{
                //makeCurrentFragment(inserisciLezioneRicorrente)
                fragment= inserisciLezioneRicorrente
                isIndividualeVisible=false
                binding.btnFragment.text="Passa a lezione singola"
            }
            bundle = Bundle()
            bundle.putString("id_aula", id_aula.toString())
            bundle.putString("nome_aula", if (nome_aula != null) nome_aula else "")
            fragment.arguments = bundle

            makeCurrentFragment(fragment)

        }

        mDbRef = Firebase.database.reference
        val eventiRef = mDbRef.child("eventi").orderByChild("id_aula").equalTo(id_aula!!.toDouble())
        var uscitaFor = false
        eventiRef.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (eventoSnapshot in snapshot.children) {
                    val evento = eventoSnapshot.getValue(Evento::class.java)
                    val timestampoggi = System.currentTimeMillis().toDouble()

                    if (Utils.isTimestampToday(evento!!.data_ora_inizio.toLong())) {
                        if (evento.data_ora_inizio <= timestampoggi && timestampoggi <= evento.data_ora_fine) {
                            binding.textStatoattuale.text= "Stato attuale: 🔴"
                            uscitaFor = true
                            break
                        } else if (evento.data_ora_inizio - 30 * 60 * 1000.0 <= timestampoggi && timestampoggi <= evento.data_ora_fine) {
                            binding.textStatoattuale.text = "Stato attuale: 🟠"
                            uscitaFor = true
                            break
                        }
                    }
                }
                if (uscitaFor == false)   binding.textStatoattuale.text= "Stato attuale: 🟢"
            }


            override fun onCancelled(error: DatabaseError) {
                Log.e("GET studenti-chat", "Errore: ${error.message}")
            }
        })




    }

    private fun makeCurrentFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.flWrapper, fragment)
            commit()
        }
    }


}