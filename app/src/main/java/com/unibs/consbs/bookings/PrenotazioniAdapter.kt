package com.unibs.consbs.bookings

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.unibs.consbs.R
import com.unibs.consbs.classrooms.AuleAdapter
import com.unibs.consbs.data.Aula
import com.unibs.consbs.data.Evento
import com.unibs.consbs.utils.Utils

class PrenotazioniAdapter(val context: Context, private val prenotazioniList: ArrayList<Evento>): RecyclerView.Adapter<PrenotazioniAdapter.PrenotazioniViewHolder>()  {

    lateinit var mAuth : FirebaseAuth


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrenotazioniViewHolder {
        mAuth = Firebase.auth
        val view = LayoutInflater.from(context).inflate(R.layout.layout_prenotazione, parent, false)
        return PrenotazioniAdapter.PrenotazioniViewHolder(view)
    }

    override fun getItemCount(): Int {
        return prenotazioniList.size
    }

    override fun onBindViewHolder(holder: PrenotazioniViewHolder, position: Int) {
        val currentPrenotazione = prenotazioniList[position]
        holder.txt_titolo_prenotazione.text = currentPrenotazione.nome
        val dataPrenotazione = Utils.timestampToFormatDate(currentPrenotazione.data_ora_inizio.toLong(), "dd/MM/yyyy")
        val orarioPrenotazioneInizio= Utils.timestampToFormatDate(currentPrenotazione.data_ora_inizio.toLong(), "HH:mm")
        val orarioPrenotazioneFine= Utils.timestampToFormatDate(currentPrenotazione.data_ora_fine.toLong(), "HH:mm")
        holder.txt_orario_prenotazione.text = dataPrenotazione + "\n"+"h "+orarioPrenotazioneInizio+" - "+orarioPrenotazioneFine

        holder.cardview.setOnClickListener{
            val intent = Intent(context, PrenotazioneDettaglioActivity::class.java )
            intent.putExtra("id_prenotazione",currentPrenotazione.id.toString())
            intent.putExtra("id_aula_prenotazione",currentPrenotazione.id_aula.toString())
            intent.putExtra("data_prenotazione",dataPrenotazione)
            intent.putExtra("orario_inizio_prenotazione",orarioPrenotazioneInizio)
            intent.putExtra("orario_fine_prenotazione",orarioPrenotazioneFine)
            intent.putExtra("id_studente_invitato", currentPrenotazione?.id_studente_invitato)

            context.startActivity(intent)
        }


    }

    class PrenotazioniViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val txt_titolo_prenotazione = itemView.findViewById<TextView>(R.id.titolo_prenotazione)
        val txt_orario_prenotazione = itemView.findViewById<TextView>(R.id.orario_prenotazione)
        val cardview = itemView.findViewById<MaterialCardView>(R.id.card_view)
    }
}