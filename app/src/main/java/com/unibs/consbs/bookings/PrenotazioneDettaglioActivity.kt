package com.unibs.consbs.bookings

import android.content.Context
import android.content.Intent
import android.opengl.Visibility
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.View
import androidx.core.view.GravityCompat
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
import com.unibs.consbs.data.Aula
import com.unibs.consbs.data.Sede
import com.unibs.consbs.data.Studente
import com.unibs.consbs.databinding.PrenotazioneDettaglioBinding
import com.unibs.consbs.utils.FirebaseUtils
import com.unibs.consbs.utils.Utils


class PrenotazioneDettaglioActivity: BaseActivity() {
    lateinit var binding: PrenotazioneDettaglioBinding
    private lateinit var mDbRef: DatabaseReference
    lateinit var db : DBHelper

    override fun onCreate(savedInstanceState: Bundle?) {

        mAuth = Firebase.auth
        val currentUser = mAuth.currentUser

        super.onCreate(savedInstanceState)
        binding = PrenotazioneDettaglioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Utils.setCurrentActivty("PrenotazioneDettaglioActivity")

        FirebaseUtils.checkUserLoggedIn(this)

        // preferenze di sessione
        sharedPreferences = getSharedPreferences("preferences", Context.MODE_PRIVATE)
        val isStudente = sharedPreferences.getBoolean("isStudente", false)

        if(!isStudente){
            binding.textElencoInvitato.visibility=View.GONE
            binding.linearElencoInvitato.visibility= View.GONE
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

        val id_aula= intent.getStringExtra("id_aula_prenotazione")
        val id_prenotazione= intent.getStringExtra("id_prenotazione")
        val data_prenotazione= intent.getStringExtra("data_prenotazione")
        val orario_inizio_prenotazione= intent.getStringExtra("orario_inizio_prenotazione")
        val orario_fine_prenotazione= intent.getStringExtra("orario_fine_prenotazione")
        val id_studente_invitato = intent.getStringExtra("id_studente_invitato")

        binding.textData.setText(Html.fromHtml("⋄ Data: <b>$data_prenotazione</b>"))
        binding.textFasciaOraria.setText(Html.fromHtml("⋄ Orario: <b>$orario_inizio_prenotazione - $orario_fine_prenotazione</b>"))

        mDbRef = Firebase.database.reference

        val pianiHashMap = HashMap<Int, String>()
        pianiHashMap[0] = "Piano Terra"
        pianiHashMap[1] = "1° Piano"
        pianiHashMap[2] = "2° Piano"

        var aulaDefinitiva=""

        mDbRef.child("aule").child(id_aula.toString()).addValueEventListener(object:ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()){
                    val aula = snapshot.getValue(Aula::class.java)
                    binding.textAula.setText(Html.fromHtml("⋄ Aula: <b>${aula?.nome.toString()}</b>"))
                    aulaDefinitiva = "${aula?.nome.toString()}"
                    binding.textPiano.setText("⋄ Piano: ${pianiHashMap[aula?.piano]}")

                    mDbRef.child("sedi").child(aula?.id_sede.toString()).addValueEventListener(object: ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if ( snapshot.exists() ) {
                                val sede = snapshot.getValue(Sede::class.java)
                                binding.textSede.setText("⋄ Sede: ${sede?.nome}")
                                binding.textIndirizzo.setText("⋄ Indirizzo: ${sede?.indirizzo}")
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.d("GET prenotazione", "Errore: ${error.message}")
                        }

                    })

                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("GET prenotazione", "Errore: ${error.message}")
            }

        })

        if(isStudente) {
            if (id_studente_invitato == null || id_studente_invitato == "") {
                if (!Utils.isDateInThePast(Utils.stringToDate("$data_prenotazione $orario_inizio_prenotazione"))) {
                    // Nessun invito
                    binding.textInvitato.text = ""
                    binding.textInvitato.visibility = View.GONE
                    binding.bottoneInvitaChat.visibility = View.VISIBLE

                    binding.bottoneInvitaChat.setOnClickListener {
                        val messaggio =
                            "Ciao! Ti invito a studiare con me nell'aula $aulaDefinitiva il giorno $data_prenotazione dalle ore $orario_inizio_prenotazione alle ore $orario_fine_prenotazione"
                        val intent = Intent(this, InvitaStudenteActivity::class.java)
                        intent.putExtra("id_evento", id_prenotazione)
                        intent.putExtra("id_aula", id_aula)
                        intent.putExtra("data_prenotazione", data_prenotazione)
                        intent.putExtra("orario_inizio_prenotazione", orario_inizio_prenotazione)
                        intent.putExtra("orario_fine_prenotazione", orario_fine_prenotazione)
                        intent.putExtra("id_studente_invitato", id_studente_invitato)
                        intent.putExtra("messaggio", messaggio)
                        startActivity(intent)
                    }
                } else {

                    binding.textInvitato.text = "⋄ Nessun invitato"
                    binding.textInvitato.visibility = View.VISIBLE
                    binding.bottoneInvitaChat.visibility = View.GONE

                }
            } else {
                // Invito presente

                binding.textInvitato.visibility = View.VISIBLE
                binding.bottoneInvitaChat.visibility = View.GONE
                mDbRef.child("studenti").child(id_studente_invitato.toString())
                    .addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                val studente = snapshot.getValue(Studente::class.java)

                                binding.textInvitato.text = "⋄ ${studente?.email}"
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.d("GET studente invitato", "Errore: ${error.message}")
                        }

                    })
            }
        }
        binding.tornaIndietro.setOnClickListener{
            val intent = Intent(this, PrenotazioniActivity::class.java)
            startActivity(intent)
            finish()
        }

    }
}