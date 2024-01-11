package com.unibs.consbs.classrooms

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import androidx.core.content.PackageManagerCompat.LOG_TAG
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.unibs.consbs.BaseActivity
import com.unibs.consbs.DBHelper
import com.unibs.consbs.R
import com.unibs.consbs.chat.ChatActivity
import com.unibs.consbs.chat.MessaggiActivity
import com.unibs.consbs.data.Aula
import com.unibs.consbs.databinding.AuleBinding
import com.unibs.consbs.databinding.ProfiloBinding
import com.unibs.consbs.utils.FirebaseUtils
import com.unibs.consbs.utils.Utils

class AuleActivity  : BaseActivity() {

    lateinit var binding: AuleBinding
    private lateinit var auleRecyclerView: RecyclerView
    private lateinit var auleList: ArrayList<Aula>
    private lateinit var adapter: AuleAdapter
    private lateinit var mDbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {

        mAuth = Firebase.auth
        val currentUser = mAuth.currentUser

        super.onCreate(savedInstanceState)
        binding = AuleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Utils.setCurrentActivty("AuleActivity")

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

        binding.dataOdierna.text = Utils.timestampToFormatDate(System.currentTimeMillis(), "dd/MM/YYYY")

        auleList = ArrayList()
        adapter = AuleAdapter(this, auleList)
        mAuth = Firebase.auth
        auleRecyclerView = binding.auleRecyclerView
        auleRecyclerView.layoutManager = LinearLayoutManager(this)
        auleRecyclerView.adapter = adapter
        mDbRef = Firebase.database.reference

        val sediHashMap = HashMap<String, String>()
        sediHashMap["Sede BS 1"] = "sede_bs1"
        sediHashMap["Sede BS 2"] = "sede_bs2"
        sediHashMap["Sede Darfo"] = "sede_dbt"

        val pianiHashMap = HashMap<String, Int>()
        pianiHashMap["Piano Terra"] = 0
        pianiHashMap["1° Piano"] = 1
        pianiHashMap["2° Piano"] = 2

        val sedeSelezionata = binding.sceltaSede.checkedChipId
        val chipSedeSelezionato = binding.sceltaSede.findViewById<Chip>(sedeSelezionata)
        var testoSedeSelezionato = chipSedeSelezionato.text.toString()

        val pianoSelezionato = binding.sceltaPiano.checkedChipId
        val chipPianoSelezionato = binding.sceltaPiano.findViewById<Chip>(pianoSelezionato)
        var testoPianoSelezionato = chipPianoSelezionato.text.toString()

        var testoRicercaNomeAula = ""

        binding.sceltaSede.setOnCheckedChangeListener { group, checkedId ->
            val chip: Chip? = group.findViewById(checkedId)
            chip.let {
                testoSedeSelezionato = chip?.text.toString()
                updateRecyclerView(
                    sediHashMap[testoSedeSelezionato]?.toString(),
                    pianiHashMap[testoPianoSelezionato]?.toInt(),
                    testoRicercaNomeAula
                )
            }
        }

        binding.sceltaPiano.setOnCheckedChangeListener { group, checkedId ->
            val chip: Chip? = group.findViewById(checkedId)
            chip.let {
                testoPianoSelezionato = chip?.text.toString()
                updateRecyclerView(
                    sediHashMap[testoSedeSelezionato]?.toString(),
                    pianiHashMap[testoPianoSelezionato]?.toInt(),
                    testoRicercaNomeAula
                )
            }
        }

        val cercaAula : EditText = binding.cercaAula
        cercaAula.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                testoRicercaNomeAula = s.toString()
                updateRecyclerView(
                    sediHashMap[testoSedeSelezionato]?.toString(),
                    pianiHashMap[testoPianoSelezionato]?.toInt(),
                    testoRicercaNomeAula
                )
            }

            override fun afterTextChanged(s: Editable?) {

            }

        })

        updateRecyclerView(
            sediHashMap[testoSedeSelezionato]?.toString(),
            pianiHashMap[testoPianoSelezionato]?.toInt(),
            testoRicercaNomeAula
        )

        binding.mostraMappa.setOnClickListener{
            val intent = Intent(this, MappaActivity::class.java)
            startActivity(intent)
            finish()
        }

    }

    private fun updateRecyclerView(sede: String?, piano: Int?, nome: String?) {

        var auleRef : Query = mDbRef.child("aule")
        auleRef.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                auleList.clear()
                val aule = snapshot.children
                    .mapNotNull { it.getValue(Aula::class.java) }
                    .filter { ( piano == null || it.piano == piano ) && ( sede == null || it.id_sede == sede ) && ( nome == "" || it.nome.toString().lowercase().contains(nome.toString().lowercase()) ) }
                    .forEach { auleList.add(it) }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("GET aule", "Errore: ${error.message}")
            }

        })
    }



}