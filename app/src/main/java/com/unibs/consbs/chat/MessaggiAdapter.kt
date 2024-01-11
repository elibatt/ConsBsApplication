package com.unibs.consbs.chat

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.unibs.consbs.R
import com.unibs.consbs.data.Messaggio

class MessaggiAdapter(val context: Context, val messaggiList: ArrayList<Messaggio>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val ITEM_SENT = 1
    val ITEM_RECEIVED = 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == 1) {
            val view: View = LayoutInflater.from(context).inflate(R.layout.sent, parent, false)
            return SentViewHolder(view)
        } else {
            val view: View = LayoutInflater.from(context).inflate(R.layout.received, parent, false)
            return ReceivedViewHolder(view)
        }
    }

    override fun getItemCount(): Int {
        return messaggiList.size
    }

    override fun getItemViewType(position: Int): Int {
        val currentMessage = messaggiList[position]
        if (FirebaseAuth.getInstance().currentUser?.uid.equals(currentMessage.id_sender)) {
            return ITEM_SENT
        } else {
            return ITEM_RECEIVED
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentMessage = messaggiList[position]
        if (holder.javaClass == SentViewHolder::class.java) {
            var viewHolder = holder as SentViewHolder
            holder.sentMessage.text = currentMessage.contenuto
        } else {
            var viewHolder = holder as ReceivedViewHolder
            holder.receivedMessage.text = currentMessage.contenuto
        }
    }

    class SentViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val sentMessage = itemView.findViewById<TextView>(R.id.txt_sent_message)
    }

    class ReceivedViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val receivedMessage = itemView.findViewById<TextView>(R.id.txt_received_message)
    }
}