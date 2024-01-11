package com.unibs.consbs.chat

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
import com.unibs.consbs.data.Messaggio
import com.unibs.consbs.data.Studente
import com.unibs.consbs.utils.Utils

class ChatAdapter(val context: Context, private val chatList: ArrayList<Chat>): RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    lateinit var mAuth: FirebaseAuth

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatAdapter.ChatViewHolder {
        mAuth = Firebase.auth
        val view = LayoutInflater.from(context).inflate(R.layout.layout_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatAdapter.ChatViewHolder, position: Int) {

        mAuth = Firebase.auth
        val currentUser = mAuth.currentUser
        val currentChat = chatList[position]

        // altro studente nella conversazione, oltre a quello loggato
        val id_studente_partecipante = if (currentUser!!.uid == currentChat.id_studente_i) currentChat.id_studente_r else currentChat.id_studente_i

        val database = Firebase.database.reference
        val studentiRef = database.child("studenti")
        var nominativo_studente = ""
        var id_device = ""

        studentiRef.child(id_studente_partecipante.toString()).addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if ( snapshot.exists() ) {
                    val studente = snapshot.getValue(Studente::class.java)
                    nominativo_studente = "${studente!!.nome} ${studente!!.cognome}"
                    id_device = studente.id_device.toString()
                    holder.txt_nominativo.text = nominativo_studente

                } else {
                    Log.e("lettura dati chat", "Nessun studente con l'id ${id_studente_partecipante.toString()}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("lettura dati chat", "Errore: ${error.message}")
            }

        })

        val messaggiRef = database.child("messaggi").orderByChild("id_chat").equalTo(currentChat.id).limitToLast(1)

        messaggiRef.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for ( messageSnapshot in snapshot.children ) {
                    val messaggio = messageSnapshot.getValue(Messaggio::class.java)

                    val timestamp_ultimo_messaggio = messaggio?.data_ora_invio!!.toLong()
                    var orario_ultimo_messaggio = ""
                    if ( Utils.isTimestampToday(timestamp_ultimo_messaggio) ) {
                        orario_ultimo_messaggio = Utils.timestampToFormatDate(timestamp_ultimo_messaggio, "HH:mm")
                    } else {
                        orario_ultimo_messaggio = Utils.timestampToFormatDate(timestamp_ultimo_messaggio)
                    }

                    holder.txt_orario.text = orario_ultimo_messaggio
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("get messaggi", "Errore: ${error.message}")
            }

        })

        holder.itemView.setOnClickListener{
            val intent = Intent(context, MessaggiActivity::class.java)
            intent.putExtra("nominativo", nominativo_studente)
            intent.putExtra("id_studente_partecipante", id_studente_partecipante)
            intent.putExtra("id_chat", currentChat.id)
            intent.putExtra("id_device", id_device)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return chatList.size
    }

    class ChatViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var txt_nominativo = itemView.findViewById<TextView>(R.id.nominativo_studente)
        var txt_orario = itemView.findViewById<TextView>(R.id.orario_ultimo_messaggio)
    }


}