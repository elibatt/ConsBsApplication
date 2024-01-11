package com.unibs.consbs.bookings

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.unibs.consbs.DBHelper
import com.unibs.consbs.R
import com.unibs.consbs.chat.ChatActivity
import com.unibs.consbs.data.Studente

class InvitaStudenteAdapter(val context: Context, private val studentiList: ArrayList<Studente>, val id_evento: String?, val messaggio: String?): RecyclerView.Adapter<InvitaStudenteAdapter.InvitaStudenteViewHolder>() {

    lateinit var mAuth: FirebaseAuth
    lateinit var db : DBHelper
    lateinit var sharedPreferences: SharedPreferences

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvitaStudenteAdapter.InvitaStudenteViewHolder {
        mAuth = Firebase.auth
        val view = LayoutInflater.from(context).inflate(R.layout.layout_studente, parent, false)
        return InvitaStudenteAdapter.InvitaStudenteViewHolder(view)
    }

    override fun onBindViewHolder(holder: InvitaStudenteAdapter.InvitaStudenteViewHolder, position: Int) {
        val currentStudente = studentiList[position]
        holder.txt_nominativo_studente.setText(Html.fromHtml(String.format("👤 <font size=\"18\">${currentStudente.nome} ${currentStudente.cognome}</font><br>📧 ${currentStudente.email}")))

        holder.card_view.setOnClickListener{
            val dialogBuilder = AlertDialog.Builder(context)

            val messageBox = "Confermi l'invito a ${currentStudente.cognome} ${currentStudente.nome}?"

            dialogBuilder.setMessage(messageBox)
                .setCancelable(false)
                .setPositiveButton("Conferma", DialogInterface.OnClickListener { dialog, id ->

                    sharedPreferences = context.getSharedPreferences("preferences", Context.MODE_PRIVATE)
                    val my_nome = sharedPreferences.getString("studente_nome", "")
                    val my_cognome = sharedPreferences.getString("studente_cognome", "")
                    val my_nominativo = "$my_nome $my_cognome"
                    val my_id_device = sharedPreferences.getString("my_id_device", "")

                    db = DBHelper()
                    db.invita_studente_a_evento(mAuth.currentUser?.uid, currentStudente.id, id_evento, my_nominativo, my_id_device, currentStudente.id_device, context,messaggio)

                    Toast.makeText(context, "Invito inviato correttamente!", Toast.LENGTH_LONG).show()

                    val resultIntent = Intent(context, PrenotazioneDettaglioActivity::class.java)
                    val invitaStudenteActivity = (context as InvitaStudenteActivity).intent
                    resultIntent.putExtra("id_aula", invitaStudenteActivity.getStringExtra("id_aula"))
                    resultIntent.putExtra("id_prenotazione", invitaStudenteActivity.getStringExtra("id_evento"))
                    resultIntent.putExtra("data_prenotazione", invitaStudenteActivity.getStringExtra("data_prenotazione"))
                    resultIntent.putExtra("orario_inizio_prenotazione", invitaStudenteActivity.getStringExtra("orario_inizio_prenotazione"))
                    resultIntent.putExtra("orario_fine_prenotazione", invitaStudenteActivity.getStringExtra("orario_fine_prenotazione"))
                    resultIntent.putExtra("id_studente_invitato", currentStudente.id)
                    context.startActivity(resultIntent)
                    dialog.dismiss()

                })
                .setNegativeButton("Annulla", DialogInterface.OnClickListener {
                        dialog, id -> dialog.cancel()
                })

            val alert = dialogBuilder.create()

            alert.setTitle("Conferma Invito")

            alert.show()
        }
    }

    override fun getItemCount(): Int {
        return studentiList.size
    }

    class InvitaStudenteViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val txt_nominativo_studente = itemView.findViewById<TextView>(R.id.nominativo_studente)
        val card_view = itemView.findViewById<MaterialCardView>(R.id.card_view)
    }
}