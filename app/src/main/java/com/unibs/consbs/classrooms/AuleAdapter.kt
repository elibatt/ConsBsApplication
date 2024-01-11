package com.unibs.consbs.classrooms

import android.content.Context
import android.content.Intent
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
import com.unibs.consbs.chat.MessaggiActivity
import com.unibs.consbs.data.Aula
import com.unibs.consbs.data.Evento
import com.unibs.consbs.utils.Utils

class AuleAdapter(val context: Context, private val auleList: ArrayList<Aula>): RecyclerView.Adapter<AuleAdapter.AuleViewHolder>() {

    lateinit var mAuth: FirebaseAuth

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AuleAdapter.AuleViewHolder {
        mAuth = Firebase.auth
        val view = LayoutInflater.from(context).inflate(R.layout.layout_aula, parent, false)
        return AuleViewHolder(view)
    }

    override fun onBindViewHolder(holder: AuleAdapter.AuleViewHolder, position: Int) {
        val currentAula = auleList[position]

        if ( !currentAula.nome.toString().isEmpty() ) {
            holder.txt_nome_aula.text = currentAula.nome.toString()
        }

        // disponibilita - da calcolare con prenotazioni e lezioni (al momento non la modifico)
        val database = Firebase.database.reference

        holder.itemView.setOnClickListener{
            val intent = Intent(context, AulaDettaglioActivity::class.java)
            intent.putExtra("id_aula", currentAula.id)
            context.startActivity(intent)
        }

        val eventiRef = database.child("eventi").orderByChild("id_aula").equalTo(currentAula.id!!.toDouble())
        var uscitaFor = false
        eventiRef.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (eventoSnapshot in snapshot.children) {
                    val evento = eventoSnapshot.getValue(Evento::class.java)
                    val timestampoggi = System.currentTimeMillis().toDouble()

                    if (Utils.isTimestampToday(evento!!.data_ora_inizio.toLong()))

                        if (evento.data_ora_inizio <= timestampoggi && timestampoggi <= evento.data_ora_fine) {
                            holder.disponibilita_aula.text= "🔴"
                            uscitaFor = true
                            break
                        } else if (evento.data_ora_inizio - 30 * 60 * 1000.0 <= timestampoggi && timestampoggi <= evento.data_ora_fine) {
                            holder.disponibilita_aula.text = "🟠"
                            uscitaFor = true
                            break
                        }
                }
                if (uscitaFor == false)  holder.disponibilita_aula.text= "🟢"
            }


            override fun onCancelled(error: DatabaseError) {
                Log.e("GET studenti-chat", "Errore: ${error.message}")
            }
        })

    }

    override fun getItemCount(): Int {
        return auleList.size
    }

    class AuleViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val txt_nome_aula = itemView.findViewById<TextView>(R.id.nome_aula)
        val disponibilita_aula = itemView.findViewById<TextView>(R.id.disponibilita)
    }

}