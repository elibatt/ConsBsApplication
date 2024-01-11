package com.unibs.consbs.classrooms

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.unibs.consbs.R
import com.unibs.consbs.data.Chat
import com.unibs.consbs.data.Evento
import com.unibs.consbs.data.Messaggio
import com.unibs.consbs.data.Studente
import com.unibs.consbs.utils.Utils
import java.util.Objects

class PopupAdapter(val context: Context, private val eventoList: ArrayList<Evento>): RecyclerView.Adapter<PopupAdapter.PopupViewHolder>() {

    lateinit var mAuth: FirebaseAuth

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PopupAdapter.PopupViewHolder{
        mAuth = Firebase.auth
        val view = LayoutInflater.from(context).inflate(R.layout.layout_booking_aula, parent, false)
        return PopupViewHolder(view)
    }

    override fun onBindViewHolder(holder: PopupAdapter.PopupViewHolder, position: Int) {

        mAuth = Firebase.auth
        val currentUser = mAuth.currentUser
        val currentEvento = eventoList[position]
        val tipologiaBooking = if(currentEvento?.prenotazione==true) "Prenotazione" else "Lezione"
        val orarioInizio  = Utils.timestampToFormatDate(currentEvento?.data_ora_inizio!!.toLong(), "HH:mm")
        val orarioFine  = Utils.timestampToFormatDate(currentEvento?.data_ora_fine!!.toLong(), "HH:mm")


        holder.txt_tipo_booking.text = tipologiaBooking
        holder.txt_orario_booking.text = orarioInizio+" - "+orarioFine

    }
    override fun getItemCount(): Int {
        return eventoList.size
    }

    class PopupViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var txt_tipo_booking = itemView.findViewById<TextView>(R.id.tipo_booking)
        var txt_orario_booking = itemView.findViewById<TextView>(R.id.orario_booking)
    }




}