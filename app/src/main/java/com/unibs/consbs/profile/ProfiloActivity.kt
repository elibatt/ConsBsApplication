package com.unibs.consbs.profile

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.opengl.Visibility
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.GravityCompat
import androidx.core.view.marginLeft
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.unibs.consbs.*
import com.unibs.consbs.data.*
import com.unibs.consbs.databinding.ProfiloBinding
import com.unibs.consbs.databinding.ProfiloProfessoreBinding
import com.unibs.consbs.utils.FirebaseUtils
import com.unibs.consbs.utils.Utils
import kotlin.collections.HashMap


class ProfiloActivity : BaseActivity() {

    lateinit var binding: ProfiloBinding
    private lateinit var db: DBHelper


    override fun onCreate(savedInstanceState: Bundle?) {

        mAuth = Firebase.auth
        val currentUser = mAuth.currentUser

        super.onCreate(savedInstanceState)

        binding = ProfiloBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Utils.setCurrentActivty("ProfiloActivity")

        FirebaseUtils.checkUserLoggedIn(this)

        binding.menu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        // preferenze di sessione
        sharedPreferences = getSharedPreferences("preferences", Context.MODE_PRIVATE)
        val isStudente = sharedPreferences.getBoolean("isStudente", false)

        if ( !isStudente ) {
            binding.rowLivelloAccademico.visibility = View.GONE
            binding.rowNumeroMatricola.visibility = View.GONE

            binding.titoloCorso.text = "Materia"

        }

        // Menù
        setNavigationViewSelectedItem("Profilo")
        setOptionsItemsListener()

        binding.swiperefresh.setOnRefreshListener {
            setOnRefreshLayer()
        }

        if ( isStudente ) {
            var id_studente : String? = currentUser!!.uid
            get_studente(id_studente)
        } else {
            var id_professore : String? = currentUser!!.uid
            get_professore(id_professore)
            Log.d("id_professore", id_professore.toString())
        }

        binding.modificaProfilo.setOnClickListener{
            val intent = Intent(this, ModificaProfiloActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun get_studente ( id_studente: String? = null ) {

        var annoMap : HashMap<Int, String> = HashMap()
        annoMap[1] = "I"
        annoMap[2] = "II"
        annoMap[3] = "III"

        val database = Firebase.database.reference
        val studenteRef = database.child("studenti")

        studenteRef.child(id_studente.toString()).addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()){
                    val studente = snapshot.getValue(Studente::class.java)

                    if ( studente != null ) {
                        binding.profiloNome.text = studente.nome
                        binding.profiloCognome.text = studente.cognome
                        binding.profiloNumeroMatricola.text = studente.matricola
                        binding.profiloEmail.text = studente.email
                        binding.profiloNumeroCellulare.text = studente.cellulare

                        val livelloStudioRef = database.child("livellostudi")

                        livelloStudioRef.child(studente.id_livellostudio.toString())
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    val livelloStudio = snapshot.getValue(LivelloStudio::class.java)

                                    if (livelloStudio != null) {

                                        binding.profiloLivelloAccademico.text = "${livelloStudio.livello} - ${annoMap[livelloStudio.anno]} anno"

                                        val corsoRef = database.child("corsi").child(livelloStudio.id_corso.toString())

                                        corsoRef.addListenerForSingleValueEvent(object: ValueEventListener{
                                            override fun onDataChange(snapshot: DataSnapshot) {
                                                if ( snapshot.exists() ) {
                                                    val corso = snapshot.getValue(Corso::class.java)

                                                    binding.profiloCorso.text = corso?.nome
                                                }
                                            }

                                            override fun onCancelled(error: DatabaseError) {
                                                Log.d("GET corso", "Errore: ${error.message}")
                                            }

                                        })
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Log.d(
                                        "Firebase",
                                        "Errore nel caricamento del livellostudi: " + error.getMessage()
                                    )
                                }
                            })

                        // salvataggio dati studente
                        sharedPreferences = getSharedPreferences("preferences", Context.MODE_PRIVATE)
                        val editor = sharedPreferences.edit()
                        editor.putString("studente_nome", studente.nome)
                        editor.putString("studente_cognome", studente.cognome)
                        editor.putString("my_id_device", studente.id_device)
                        editor.apply()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("Firebase", "Errore nel caricamento dello studente: " + error.getMessage())
            }
        })
    }

    private fun get_professore ( id_professore: String? = null ) {

        val mDbRef = Firebase.database.reference

        mDbRef.child("professori").child(id_professore.toString()).addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if ( snapshot.exists() ) {
                    val professore = snapshot.getValue(Professore::class.java)

                    if ( professore != null ) {
                        binding.profiloNome.text = professore.nome
                        binding.profiloCognome.text = professore.cognome
                        binding.profiloEmail.text = professore.email
                        binding.profiloNumeroCellulare.text = professore.cellulare

                        mDbRef.child("professori_materie").orderByChild("id_professore").equalTo(id_professore.toString()).addListenerForSingleValueEvent(object: ValueEventListener{
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val listaIdMaterie = snapshot.children
                                                     .mapNotNull { it.getValue(Professore_Materia::class.java) }
                                                     .map { it.id_materia }

                                binding.profiloCorso.text = ""
                                for ( id_materia in listaIdMaterie ) {

                                    mDbRef.child("materie").child(id_materia.toString()).addListenerForSingleValueEvent(object: ValueEventListener{
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            if ( snapshot.exists() ) {
                                                val materia = snapshot.getValue(Materia::class.java)
                                                binding.profiloCorso.text = " - ${materia?.nome}\n${binding.profiloCorso.text}"
                                            }

                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            Log.d("GET materie", "Errore: ${error.message}")
                                        }

                                    })

                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.d("GET professori_materie", "Errore: ${error.message}")
                            }

                        })

                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("GET professori", "Errore: ${error.message}")
            }

        })

    }

}