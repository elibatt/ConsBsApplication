package com.unibs.consbs.profile

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.unibs.consbs.BaseActivity
import com.unibs.consbs.DBHelper
import com.unibs.consbs.Studente_Materia
import com.unibs.consbs.classrooms.AuleAdapter
import com.unibs.consbs.data.Aula
import com.unibs.consbs.data.Materia
import com.unibs.consbs.databinding.ModificaMaterieBinding
import com.unibs.consbs.databinding.ModificaProfiloBinding
import com.unibs.consbs.utils.FirebaseUtils
import com.unibs.consbs.utils.Utils

class ModificaMaterieActivity: BaseActivity() {

    lateinit var binding: ModificaMaterieBinding
    private lateinit var db: DBHelper
    private lateinit var mDbRef: DatabaseReference
    private lateinit var materieRecyclerView: RecyclerView
    private lateinit var materieList: ArrayList<Materia>
    private lateinit var adapter: ModificaMaterieAdapter
    private lateinit var materieListComplete: ArrayList<Materia>

    override fun onCreate(savedInstanceState: Bundle?) {

        mAuth = Firebase.auth
        val currentUser = mAuth.currentUser

        super.onCreate(savedInstanceState)
        binding = ModificaMaterieBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Utils.setCurrentActivty("ModificaMaterieActivity")

        FirebaseUtils.checkUserLoggedIn(this)

        binding.menu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        // Menù
        setNavigationViewSelectedItem("Profilo")
        setOptionsItemsListener()

        mDbRef = Firebase.database.reference

        materieList = ArrayList()
        adapter = ModificaMaterieAdapter(this, materieList)
        materieRecyclerView = binding.materieRecyclerView
        materieRecyclerView.layoutManager = LinearLayoutManager(this)
        materieRecyclerView.adapter = adapter

        var sceltaSelezione : String? = ""
        var testoRicercaMateria = ""

        binding.sceltaSelezione.setOnCheckedChangeListener{ group, checkedId ->
            val chip: Chip? = group.findViewById(checkedId)
            chip.let {
                sceltaSelezione = chip?.text.toString().lowercase().trim()
                if ( sceltaSelezione != "selezionate" && sceltaSelezione != "non selezionate" ) sceltaSelezione = ""
                updateRecyclerView(
                    sceltaSelezione.toString(),
                    testoRicercaMateria
                )
            }
        }

        val cercaMaterie : EditText = binding.cercaMaterie
        cercaMaterie.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                testoRicercaMateria = s.toString().lowercase().trim()
                updateRecyclerView(
                    sceltaSelezione.toString(),
                    testoRicercaMateria
                )
            }

            override fun afterTextChanged(s: Editable?) {

            }

        })

        materieListComplete = ArrayList()

        updateRecyclerView(sceltaSelezione.toString(), testoRicercaMateria)

        val selectedMaterie: MutableList<Materia> = mutableListOf()
        binding.salvaModifiche.setOnClickListener{
            db = DBHelper()

            for ( materia in materieListComplete ) {
                if ( materia.isSelected ) {
                    db.associa_studente_materia(currentUser?.uid, materia.id)
                } else {
                    db.rimuovi_associazione_studente_materia(currentUser?.uid, materia.id)
                }
            }

            val intent = Intent(this@ModificaMaterieActivity, ModificaMaterieActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun updateRecyclerView(sceltaSelezione: String, testoRicercaMateria: String) {
        mDbRef.child("studenti_materie").orderByChild("id_studente").equalTo(mAuth.currentUser?.uid.toString()).addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                materieList.clear()
                materieListComplete.clear()
                val listaMaterie = snapshot.children
                    .mapNotNull { it.getValue(Studente_Materia::class.java) }
                    .map { it.id_materia }

                mDbRef.child("materie").addListenerForSingleValueEvent(object: ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for ( materiaSnapshot in snapshot.children ) {
                            val materia = materiaSnapshot.getValue(Materia::class.java)
                            materieListComplete.add(materia!!)

                            if ( testoRicercaMateria == "" || materia?.nome.toString().lowercase().contains(testoRicercaMateria) ) {
                                if ( sceltaSelezione == "" ) materieList.add(materia!!)
                                else if ( sceltaSelezione == "selezionate" && listaMaterie.contains(materia?.id?.toInt()) ) materieList.add(materia!!)
                                else if ( sceltaSelezione == "non selezionate" && !listaMaterie.contains(materia?.id?.toInt()) ) materieList.add(materia!!)
                            }
                        }
                        adapter.notifyDataSetChanged()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.d("GET materie", "Errore: ${error.message}")
                    }

                })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("GET studenti_materie", "Errore: ${error.message}")
            }

        })

    }
}