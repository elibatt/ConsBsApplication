package com.unibs.consbs.chat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.unibs.consbs.DBHelper
import com.unibs.consbs.data.Messaggio
import com.unibs.consbs.databinding.MessaggiBinding
import com.unibs.consbs.utils.Utils
import org.json.JSONObject
import java.util.HashMap

class MessaggiActivity: AppCompatActivity() {

    private lateinit var binding: MessaggiBinding
    private lateinit var messaggiRecyclerView: RecyclerView
    private lateinit var messageBox: EditText
    private lateinit var sendButton: ImageView
    private lateinit var messageAdapter: MessaggiAdapter
    private lateinit var messaggiList: ArrayList<Messaggio>
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        mAuth = Firebase.auth
        mDbRef = Firebase.database.reference

        super.onCreate(savedInstanceState)
        binding = MessaggiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Utils.setCurrentActivty("MessaggiActivity")

        // da ChatActivity
        var nominativo = intent.getStringExtra("nominativo")
        val id_chat = intent.getStringExtra("id_chat")
        val id_studente_partecipante = intent.getStringExtra("id_studente_partecipante")
        val id_sender = mAuth.currentUser?.uid
        var id_device = intent.getStringExtra("id_device")

        binding.personaChat.text = nominativo

        messaggiList = ArrayList()
        messageAdapter = MessaggiAdapter(this, messaggiList)

        messaggiRecyclerView = binding.messaggiRecyclerView

        messaggiRecyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true // Impostazione per invertire l'ordine di visualizzazione
        }
        messaggiRecyclerView.adapter = messageAdapter

        val messaggiRef = mDbRef.child("messaggi").orderByChild("id_chat").equalTo(id_chat)

        messaggiRef.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                messaggiList.clear()
                for ( messaggioSnapshot in snapshot.children ) {
                    val messaggio = messaggioSnapshot.getValue(Messaggio::class.java)
                    messaggiList.add(messaggio!!)
                }
                messageAdapter.notifyDataSetChanged()
                messaggiRecyclerView.scrollToPosition(messaggiList.size-1)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("get messaggi", "Errore: ${error.message}")
            }

        })

        binding.arrowBack.setOnClickListener{
            val intent = Intent(this, ChatActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.sendButton.setOnClickListener{
            val message = binding.messageBox.text.toString()
            if(message != "") {

                val db = DBHelper()
                db.aggiungi_messaggio(
                    id_chat,
                    id_sender,
                    id_studente_partecipante,
                    message
                )

                if (id_device != "") {

                    val sharedPreferences = getSharedPreferences("preferences", Context.MODE_PRIVATE)
                    val my_nome = sharedPreferences.getString("studente_nome", "")
                    val my_cognome = sharedPreferences.getString("studente_cognome", "")
                    val my_nominativo = "$my_nome $my_cognome"
                    val my_id_device = sharedPreferences.getString("my_id_device", "")

                    inviaNotificaPush(id_device.toString(), "Nuovo messaggio da $my_nominativo", message, id_chat.toString(), my_nominativo, id_sender.toString(), my_id_device.toString())
                }

                binding.messageBox.setText("")
            }
        }
    }

    fun inviaNotificaPush(token: String, titoloNotifica: String, testoNotifica: String, id_chat: String, nominativo: String, id_studente_partecipante: String, my_id_device: String) {

        val apikey = "AAAARNotFsg:APA91bHtN_zRrskdQZ0nH7xJsOIEh2BkE-cPkYYYQGx6kkheVG6ejgY78x1z9Sqq1wHEQd1_pZePHaOuf15h9ZWXnjxyOlF-CjdJ422BHJHIsta-BgRH4bENv957P1i_SQBkDG900L2E"

        val notification = JSONObject()
        val notifData = JSONObject()

        notifData.put("title", titoloNotifica)
        notifData.put("body", testoNotifica)
        notifData.put("id_chat", id_chat)
        notifData.put("action","OPEN_ACTIVITY")
        notifData.put("nominativo", nominativo)
        notifData.put("id_studente_partecipante", id_studente_partecipante)
        notifData.put("id_device", my_id_device)


        val action = JSONObject()
        action.put("action", "open_activity")
        action.put("title", "Apri l'app")
        notifData.put("actions", action)

        notification.put("to", token)
        notification.put("notification", notifData)
        notification.put("data", notifData)
        notification.put("clickAction", "OPEN_ACTIVITY")
        notification.put("id_chat", id_chat)


        val requestQueue = Volley.newRequestQueue(this)
        val url = "https://fcm.googleapis.com/fcm/send"

        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST, url, notification,
            Response.Listener<JSONObject> { response ->
                // Notifica inviata con successo
                Log.d("Notifica", "Inviata con successo al device $token")
            },
            Response.ErrorListener { error ->
                // Errore nell'invio della notifica
                Log.d("Notifica", "Errore: $error")
            }) {
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "key=$apikey"
                headers["Content-Type"] = "application/json"
                return headers
            }
        }

        requestQueue.add(jsonObjectRequest)
    }

}