package com.unibs.consbs.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.unibs.consbs.profile.ProfiloActivity
import com.unibs.consbs.R
import com.unibs.consbs.bookings.PrenotazioniActivity
import com.unibs.consbs.chat.ChatActivity
import com.unibs.consbs.chat.MessaggiActivity

const val channelId = "notification_channel"
const val channelName = "com.unibs.consbs.firebasenotifications"

class MyFirebaseMessagingService: FirebaseMessagingService() {


    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val currentActivityName = Utils.getCurrentActivity()
        if ( currentActivityName != "MessaggiActivity" ) {
            if (remoteMessage.notification != null) {
                if(remoteMessage.data["mode"].toString()=="NOCHAT"){
                    generateNotificationNoChat(
                    remoteMessage.notification!!.title!!,
                    remoteMessage.notification!!.body!!,

                    )

                }else{
                    generateNotification(
                        remoteMessage.notification!!.title!!,
                        remoteMessage.notification!!.body!!,
                        remoteMessage.data["id_chat"]!!.toString(),
                        remoteMessage.data["action"]!!.toString(),
                        remoteMessage.data["nominativo"]!!.toString(),
                        remoteMessage.data["id_studente_partecipante"]!!.toString(),
                        remoteMessage.data["id_device"].toString()
                    )
                }

            }
        }
    }

    private fun generateNotificationNoChat(title: String, message: String) {
        var intent = Intent(this, PrenotazioniActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)

        var builder: NotificationCompat.Builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(1000, 1000, 1000, 1000))
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setContentTitle(title)
            .setContentText(message)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val notificationChannel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        notificationManager.notify(0, builder.build())
    }



    fun generateNotification(title: String, message: String, id_chat: String, activityToOpen: String, nominativo: String, id_studente_partecipante: String, id_device: String) {

        var intent = Intent(this, MessaggiActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if ( !activityToOpen.isEmpty() || !id_chat.isEmpty() ) {
            intent.putExtra("action", activityToOpen)
            intent.putExtra("id_chat", id_chat)
            intent.putExtra("nominativo", nominativo)
            intent.putExtra("id_studente_partecipante", id_studente_partecipante)
            intent.putExtra("id_device", id_device)
        } else {
            intent = Intent(this, ChatActivity::class.java)
        }

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)

        var builder: NotificationCompat.Builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(1000, 1000, 1000, 1000))
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setContentTitle(title)
            .setContentText(message)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val notificationChannel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        notificationManager.notify(0, builder.build())
    }

}