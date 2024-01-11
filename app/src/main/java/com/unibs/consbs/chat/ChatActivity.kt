package com.unibs.consbs.chat

import android.os.Bundle
import android.util.Log
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.unibs.consbs.BaseActivity
import com.unibs.consbs.data.Chat
import com.unibs.consbs.data.Messaggio
import com.unibs.consbs.databinding.ChatBinding
import com.unibs.consbs.utils.Utils

class ChatActivity: BaseActivity() {

    lateinit var binding: ChatBinding
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var chatList: ArrayList<Chat>
    private lateinit var adapter: ChatAdapter
    private lateinit var mDbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Utils.setCurrentActivty("ChatActivity")

        binding.menu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        // Menù
        setNavigationViewSelectedItem("Chat")
        setOptionsItemsListener()

        binding.swiperefresh.setOnRefreshListener {
            setOnRefreshLayer()
        }

        chatList = ArrayList()
        adapter = ChatAdapter(this, chatList)
        mAuth = Firebase.auth
        chatRecyclerView = binding.chatRecyclerView
        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = adapter
        mDbRef = Firebase.database.reference

        val currentUser = mAuth.currentUser

        mDbRef.child("chat").addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                chatList.clear()
                for ( chatSnapshot in snapshot.children ) {
                    val chat = chatSnapshot.getValue(Chat::class.java)
                    if ( chat?.id.toString().contains(currentUser?.uid.toString()) ) {
                        chatList.add(chat!!)

                        val messaggiRef = mDbRef.child("messaggi").orderByChild("id_chat").equalTo(chat.id).limitToLast(1)
                        messaggiRef.addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                for (messageSnapshot in snapshot.children) {
                                    val messaggio = messageSnapshot.getValue(Messaggio::class.java)
                                    val timestampUltimoMessaggio = messaggio?.data_ora_invio!!
                                    chat.timestamp_ultimo_messaggio = timestampUltimoMessaggio.toDouble()
                                }
                                chatList.sortByDescending { it.timestamp_ultimo_messaggio }
                                adapter.notifyDataSetChanged()
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("get messaggi", "Errore: ${error.message}")
                            }
                        })

                    }
                }

                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("GET studenti-chat", "Errore: ${error.message}")
            }

        })

    }

}