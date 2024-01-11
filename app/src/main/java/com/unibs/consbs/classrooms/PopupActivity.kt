package com.unibs.consbs.classrooms

import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.unibs.consbs.data.Evento
import com.unibs.consbs.databinding.PopupBinding
import com.unibs.consbs.databinding.PrenotazioneAulaBinding
import com.unibs.consbs.utils.FirebaseUtils
import com.unibs.consbs.utils.Utils
import java.text.SimpleDateFormat

class PopupActivity: AppCompatActivity() {
    lateinit var binding: PopupBinding
    private lateinit var mDbRef: DatabaseReference
    private lateinit var popupRecyclerView: RecyclerView
    private lateinit var eventoList: ArrayList<Evento>
    private lateinit var adapter: PopupAdapter
    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        binding = PopupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Utils.setCurrentActivty("PopupActivity")

        FirebaseUtils.checkUserLoggedIn(this)

        //settiamo il popup con displaymetrics
        val displaymetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displaymetrics)
        val width= displaymetrics.widthPixels
        val height= displaymetrics.heightPixels
        window.setLayout((width*.8).toInt(),(height*.7).toInt())
        var params:WindowManager.LayoutParams = window.attributes
        params.gravity=Gravity.CENTER
        params.x=0
        params.y=-20
        window.attributes=params

        eventoList = ArrayList()
        adapter = PopupAdapter(this, eventoList)
        popupRecyclerView = binding.auleDettaglioRecyclerView
        popupRecyclerView.layoutManager = LinearLayoutManager(this)
        popupRecyclerView.adapter = adapter
        mDbRef = Firebase.database.reference

        val id_aula = intent.getIntExtra("id_aula",0)
        val nome_aula = intent.getStringExtra("nome_aula")
        val data_prenotazione = intent.getStringExtra("data_prenotazione")

        binding.eventiAula.text = "Eventi $nome_aula\nData: $data_prenotazione"

        val eventiRef = mDbRef.child("eventi").orderByChild("id_aula").equalTo(id_aula!!.toDouble())
        eventiRef.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                eventoList.clear()
                for ( eventoSnapshot in snapshot.children ) {
                    val evento = eventoSnapshot.getValue(Evento::class.java)
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy")
                    if(Utils.checkTimestampDay(evento!!.data_ora_inizio.toLong(), dateFormat.parse(data_prenotazione)))
                        eventoList.add(evento!!)

                }
                adapter.notifyDataSetChanged()


            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("GET popup", "Errore: ${error.message}")
            }

        })

    }
}