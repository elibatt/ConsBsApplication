package com.unibs.consbs.utils

import android.content.Context
import android.util.Log
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.*

object Utils {

    private var current_activity : String = ""

    fun setCurrentActivty(current_activity: String) {
        this.current_activity = current_activity
    }

    fun getCurrentActivity(): String {
        return this.current_activity
    }

    fun timestampToDate(timestamp: Long): Date {
        return Date(timestamp)
    }

    fun timestampToFormatDate(timestamp: Long, pattern: String = "dd/MM HH:mm"): String {
        val dateTime = Date(timestamp)
        isDateToday(dateTime)
        val dateFormat = SimpleDateFormat(pattern, Locale.getDefault())
        return dateFormat.format(dateTime)
    }

    fun formatDateToTimestamp(date: String, pattern: String = "dd/MM/yyyy HH:mm"):Long{
        val data = SimpleDateFormat(pattern).parse(date)
        return data.time
    }

    fun stringToDate(dateString: String, pattern: String = "dd/MM/yyyy HH:mm"): Date {
        val format = SimpleDateFormat(pattern)
        return format.parse(dateString)
    }

    fun isTimestampToday(timestamp: Long): Boolean {
        val datetime = timestampToDate(timestamp)
        return isDateToday(datetime)
    }

    fun isDateToday(date: Date): Boolean {
        val currentDate = Date()
        return date.year == currentDate.year && date.month == currentDate.month && date.date == currentDate.date
    }

    fun isTimestampTodayOrAfter(timestamp: Long): Boolean {
        val datetime = timestampToDate(timestamp)
        val currentDate = Date()
        return datetime.after(currentDate) || isDateToday(datetime)
    }

    fun checkTimestampDay(timestamp: Long, date:Date): Boolean{
        val datetime= timestampToDate(timestamp)
        return date.year == datetime.year && date.month == datetime.month && date.date == datetime.date
    }

    fun isDateInThePast(date: Date): Boolean {
        val currentDate = Date()
        return date.before(currentDate)
    }

    fun countDigits(num: Int): Int {
        if ( num == 0 ) return 1

        var myInput = num
        var count : Int = 0
        while (myInput != 0) {
            myInput /= 10
            ++count
        }
        return count
    }

    fun formatTimeQuarter(hour: Int, minute: Int): Pair<Int, Int> {
        var return_minute = minute
        var return_hour = hour

        if ( minute == 0 ) return_minute = 0
        else if ( minute in 1 ..15 ) return_minute = 15
        else if ( minute in 16 .. 30 ) return_minute = 30
        else if ( minute in 31 .. 45 ) return_minute = 45
        else {
            return_minute = 0
            return_hour += 1
        }

        return Pair(return_hour, return_minute)
    }

    fun inviaNotificaPush(context: Context, token: String, titoloNotifica: String, testoNotifica: String, id_chat: String, nominativo: String, id_studente_partecipante: String, my_id_device: String) {

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


        val requestQueue = Volley.newRequestQueue(context)
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

    fun isPhoneNumberValid(phoneNumber: String): Boolean {
        val pattern = "^[+]?(?:[0-9]●?){6,14}[0-9]$".toRegex()
        return phoneNumber.matches(pattern)
    }

    fun isEmailValid(email: String): Boolean {
        val emailPattern = Regex("^\\w+([.-]?\\w+)*@\\w+([.-]?\\w+)*(\\.\\w{2,3})+$")
        return email.matches(emailPattern)
    }

    fun inviaNotificaPushNoChat(context: Context, token: String, titoloNotifica: String, testoNotifica: String) {

        val apikey = "AAAARNotFsg:APA91bHtN_zRrskdQZ0nH7xJsOIEh2BkE-cPkYYYQGx6kkheVG6ejgY78x1z9Sqq1wHEQd1_pZePHaOuf15h9ZWXnjxyOlF-CjdJ422BHJHIsta-BgRH4bENv957P1i_SQBkDG900L2E"

        val notification = JSONObject()
        val notifData = JSONObject()

        notifData.put("title", titoloNotifica)
        notifData.put("body", testoNotifica)
        notifData.put("action","OPEN_ACTIVITY")
        notifData.put("mode", "NOCHAT")



        val action = JSONObject()
        action.put("action", "open_activity")
        action.put("title", "Apri l'app")
        notifData.put("actions", action)

        notification.put("to", token)
        notification.put("notification", notifData)
        notification.put("data", notifData)
        notification.put("clickAction", "OPEN_ACTIVITY")



        val requestQueue = Volley.newRequestQueue(context)
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