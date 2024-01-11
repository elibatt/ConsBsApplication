package com.unibs.consbs.lectures

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.unibs.consbs.R
import com.unibs.consbs.bookings.PrenotazioneDettaglioActivity
import com.unibs.consbs.bookings.PrenotazioniAdapter
import com.unibs.consbs.data.Aula
import com.unibs.consbs.data.Evento
import com.unibs.consbs.utils.Utils

class LezioniAdapter(val context: Context, private val lezioniList: ArrayList<Evento>): RecyclerView.Adapter<LezioniAdapter.LezioniViewHolder>()  {
    lateinit var mAuth : FirebaseAuth
    lateinit var mDbRef: DatabaseReference


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LezioniViewHolder {
        mDbRef = Firebase.database.reference
        mAuth = Firebase.auth
        val view = LayoutInflater.from(context).inflate(R.layout.layout_lezione, parent, false)
        return LezioniAdapter.LezioniViewHolder(view)
    }

    override fun getItemCount(): Int {
        return lezioniList.size
    }

    override fun onBindViewHolder(holder: LezioniViewHolder, position: Int) {
        val currentLezione = lezioniList[position]
        holder.txt_titolo_lezione.text = currentLezione.nome

        val sediHashMap = HashMap<String, String>()
        sediHashMap["sede_bs1"] = "Sede BS1"
        sediHashMap["sede_bs2"] = "Sede BS2"
        sediHashMap["sede_dbt"] = "Sede Darfo Boario"

        val auleRef = mDbRef.child("aule").child(currentLezione.id_aula.toString())
        auleRef.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
               if(snapshot.exists()){
                   val aula = snapshot.getValue(Aula::class.java)
                   holder.txt_aula_sede_lezione.text="Aula "+aula?.nome.toString()+" - "+sediHashMap[aula?.id_sede.toString()]
               }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("Errore ciclo eventi","Ciclo lezioni")
            }

        } )

        val orarioPrenotazioneInizio= Utils.timestampToFormatDate(currentLezione.data_ora_inizio.toLong(), "HH:mm")
        val orarioPrenotazioneFine= Utils.timestampToFormatDate(currentLezione.data_ora_fine.toLong(), "HH:mm")
        holder.txt_orario_lezione.text =orarioPrenotazioneInizio+" - "+orarioPrenotazioneFine




    }

    class LezioniViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val txt_titolo_lezione = itemView.findViewById<TextView>(R.id.nominativo_lezione)
        val txt_aula_sede_lezione=itemView.findViewById<TextView>(R.id.lezione_aula_sede)
        val txt_orario_lezione = itemView.findViewById<TextView>(R.id.orario_lezione)
        val cardview = itemView.findViewById<MaterialCardView>(R.id.card_view)
    }
}