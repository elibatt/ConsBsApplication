package com.unibs.consbs

import android.content.ContentValues.TAG
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.unibs.consbs.data.*
import com.unibs.consbs.utils.Utils
import java.text.SimpleDateFormat
import java.util.*



/*
    Classi Per DB
 */

data class Lezione(val id: Int? = null, val id_aula: Int? = null, val id_materia: Int? = null, val data_ora_inizio: Date? = null, val data_ora_fine: Date? = null, val aggiunta: Boolean? = null) {}

data class Materia_LivelloStudio(val id: Int? = null, val id_materia: Int? = null, val id_livellostudio: Int? = null) {
    override fun toString(): String {
        return "Materia_LivelloStudio(id=$id, id_materia=$id_materia, id_livellostudio=$id_livellostudio)"
    }
}

data class Professore_Materia(val id: Int? = null, val id_professore: String? = null, val id_materia: Int? = null)

data class Studente_Materia(val id_studente: String? = null, val id_materia: Int? = null) {}

data class Prenotazione(val id: Int? = null, val id_studente_creatore: String? = null, val id_aula: Int? =  null, val data_ora_inizio: Date? = null, val data_ora_fine: Date? = null) {}

data class Chat_Prenotazione(val id: Int? = null, val id_chat: String? = null, val id_prenotazione: Int? = null) {}


class DBHelper {

    val database = Firebase.database.reference
    val databaseIns = FirebaseDatabase.getInstance()
    val DB_URL = "https://consbs-2358d-default-rtdb.europe-west1.firebasedatabase.app/"

    fun aggiungi_corso(id_corso: Int? = null, nome: String? = null) {
        val corsoRef = database.child("corsi")

        val corso = Corso(id_corso, nome)
        corsoRef.child(id_corso.toString()).setValue(corso)
    }

    fun aggiungi_materia(id_materia: Int? = null, nome: String? = null) {
        val materieRef = database.child("materie")

        val materia = Materia(id_materia, nome)
        materieRef.child(id_materia.toString()).setValue(materia)
    }

    fun aggiungi_evento( id_evento: String? = null, nome: String? = null,  id_user_inserimento: String? = null,  id_aula: Int? = null,
                         data_ora_inizio: Double = 0.0, data_ora_fine:Double = 0.0,  prenotazione: Boolean?=null,
                         id_studente_invitato: String? = null, id_materia: String?=null, ricorrente:Boolean?=false) {
        val eventiRef = database.child("eventi")

        var evento = Evento(id_evento, nome,  id_user_inserimento,  id_aula, data_ora_inizio, data_ora_fine,  prenotazione, id_studente_invitato, id_materia)

        val eventoRef = eventiRef.push()
        eventoRef.setValue(evento)

        evento.id = eventoRef.key.toString()
        eventiRef.child(eventoRef.key.toString()).setValue(evento)

    }

    fun invita_studente_a_evento( id_user: String? = null, id_user_invitato: String? = null, id_evento: String? = null, nominativo_user: String? = null, id_device_user: String? = null, id_device_user_invitato: String? = null, context: Context, messaggio:String? ) {
        val eventiRef = database.child("eventi")

        eventiRef.child(id_evento.toString()).child("id_studente_invitato").setValue(id_user_invitato)

        val chatRef = database.child("chat")
        chatRef.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                var id_chat = ""
                for(chatSnapshot in snapshot.children) {
                    val chat = chatSnapshot.getValue(Chat::class.java)
                    if ( chat?.id.toString().contains(id_user.toString()) && chat?.id.toString().contains(id_user_invitato.toString()) ) {
                        id_chat = chat?.id.toString()
                    }
                }

                if ( id_chat != "" ) {
                    // manda mess
                    aggiungi_messaggio(
                        id_chat, id_user, id_user_invitato, messaggio
                    )

                    Utils.inviaNotificaPush(
                        context, id_device_user_invitato.toString(), "Nuovo messaggio da $nominativo_user", "Nuovo invito", id_chat, nominativo_user.toString(), id_user_invitato.toString(), id_device_user.toString()
                    )

                } else {
                    id_chat = id_user+"-"+id_user_invitato
                    val chat = Chat(id_chat, id_user, id_user_invitato)

                    chatRef.child(id_chat).setValue(chat)
                    /*
                    aggiungi_messaggio(
                        id_chat, id_user, id_user_invitato, messaggio
                    )
                    Utils.inviaNotificaPush(
                        context, id_device_user_invitato.toString(), "Nuovo messaggio da $nominativo_user", "Nuovo invito", id_chat, nominativo_user.toString(), id_user_invitato.toString(), id_device_user.toString()
                    )*/


                }

            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("cerca chat", "Errore: ${error.message}")
            }

        })
    }

    fun ottieni_materie() {
        val materieRef = database.child("materie")

        materieRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(materiaSnapshot: DataSnapshot) {

                val materieList = materiaSnapshot.children
                            .mapNotNull { it.getValue(Materia::class.java) }

                        Log.d(TAG, "Materie: $materieList")
                    }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    fun aggiungi_studente(id: String? = null, nome: String? = null, cognome: String? = null, email: String? = null, password: String? = null, cellulare: String? = null, id_livellostudio: Int? = null, matricola: String? = null) {
        val studentiRef = database.child("studenti")

        val studente = Studente(id, nome, cognome, email, password, cellulare, id_livellostudio, matricola)
        studentiRef.child(id.toString()).setValue(studente)

    }

    fun aggiungi_device(uid: String? = null, id_device: String?= null) {
        val studentiRef = database.child("studenti").child(uid.toString())

        val aggiornamenti: Map<String, String> = mapOf(
            "id_device" to id_device.toString()
        )

        studentiRef.updateChildren(aggiornamenti)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("aggiornamento device", "Andato a buon fine")
                } else {
                    Log.d("aggiornamento device","Errore durante l'aggiornamento: ${task.exception}")
                }
            }

    }

    fun associa_studente_materia(id_studente: String? = null, id_materia: Int? = null) {
        val studenti_materieRef = database.child("studenti_materie")

        val studente_materia = Studente_Materia(id_studente, id_materia)

        studenti_materieRef.child(id_studente.toString()+"-"+id_materia.toString()).setValue(studente_materia)
    }

    fun rimuovi_associazione_studente_materia(id_studente: String? = null, id_materia: Int? = null) {
        val studenti_materieRef = database.child("studenti_materie")

        studenti_materieRef.child(id_studente.toString()+"-"+id_materia).removeValue()
    }

    fun aggiungi_messaggio(id_chat: String? = null, id_sender: String? = null, id_receiver: String? = null, contenuto: String? = null) {
        val messaggiRef = database.child("messaggi")

        val messaggio = Messaggio(id_chat, id_sender, id_receiver, contenuto, System.currentTimeMillis().toDouble())

        messaggiRef.push().setValue(messaggio)
    }

    fun aggiorna_cellulare_user(id_user: String? = null, numero_cellulare: String? = null) {
        val studentiRef = database.child("studenti")

        studentiRef.child(id_user.toString()).child("cellulare").setValue(numero_cellulare.toString())
    }

    fun associa_studente_a_materie(id_studente: String? = null) {
        val database = Firebase.database.reference

        val studenteRef = database.child("studenti").child(id_studente.toString())

        studenteRef.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if ( snapshot.exists() ) {
                    val studente = snapshot.getValue(Studente::class.java)
                    val materia_livellostudioRef = database.child("materie_livellostudio").orderByChild("id_livellostudio").equalTo(studente?.id_livellostudio!!.toDouble())

                    materia_livellostudioRef.addListenerForSingleValueEvent(object: ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            var listaMateria = snapshot.children
                                .mapNotNull { it.getValue(Materia_LivelloStudio::class.java) }
                                .map { it.id_materia }

                            Log.d("listaMateria", listaMateria.toString())

                            listaMateria.forEach{
                                    ele -> associa_studente_materia(id_studente, ele)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.d("GET materia_livellostudio", "Errore: ${error.message}")
                        }

                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("GET studente", "Errore: ${error.message}")
            }

        })

    }

    fun rimuovi_singolo_evento(id_evento: String?){
        val database = Firebase.database.reference
        val eventoDaCancellareRef = database.child("eventi")
        eventoDaCancellareRef.child(id_evento.toString()).removeValue()
        Log.d("ho rimosso singolo evento"," sisi")

    }

    fun rimuovi_eventi_arcoTemporale(contextC:Context, timestampInizioPrenotz:Long, timestampFinePrenotz:Long, id_aula:String?, id_professore:String?, dataOraInizioText:String?){
        val database = Firebase.database.reference
        val eventiRef= database.child("eventi").orderByChild("id_aula").equalTo(id_aula!!.toDouble())
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm")

        eventiRef.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (eventoSnapshot in snapshot.children) {
                    val eventoPresente = eventoSnapshot.getValue(Evento::class.java)
                    //se l'evento è una prenotazione di uno studente, allora mi accingo a eliminarla per metterci la lezione

                    if(eventoPresente?.prenotazione==true) {

                        // Controllo Conflitti con Eventi
                        if (Utils.checkTimestampDay(eventoPresente!!.data_ora_inizio.toLong(), dateFormat.parse(dataOraInizioText.toString()))) {
                            if (timestampInizioPrenotz >= eventoPresente.data_ora_inizio && timestampFinePrenotz <= eventoPresente.data_ora_fine) {
                                //la lezione dell'utente è parzialmente o interamente contenuta nel range di tempo dell'evento
                                rimuovi_singolo_evento(eventoPresente.id)
                                mandaNotificaStudente(contextC,eventoPresente.id_user_inserimento)
                                break
                            } else if (timestampInizioPrenotz < eventoPresente.data_ora_inizio && timestampFinePrenotz <= eventoPresente.data_ora_fine && timestampFinePrenotz > eventoPresente.data_ora_inizio) {
                                //la lezione dell'utente inizia prima dell'evento e finisce durante o esattamente alla fine dell'evento
                                rimuovi_singolo_evento(eventoPresente.id)
                                mandaNotificaStudente(contextC,eventoPresente.id_user_inserimento)
                                break
                            } else if (timestampInizioPrenotz >= eventoPresente.data_ora_inizio && timestampInizioPrenotz < eventoPresente.data_ora_fine && timestampFinePrenotz > eventoPresente.data_ora_fine) {
                                //la lezione dell'utente inizia esattamente all'inizio o durante l'evento, e finisce dopo la fine dell'evento
                                rimuovi_singolo_evento(eventoPresente.id)
                                mandaNotificaStudente(contextC,eventoPresente.id_user_inserimento)
                                break
                            } else if (timestampInizioPrenotz < eventoPresente.data_ora_inizio && timestampFinePrenotz > eventoPresente.data_ora_fine) {
                                // la lezione ingloba un evento
                                rimuovi_singolo_evento(eventoPresente.id)
                                mandaNotificaStudente(contextC,eventoPresente.id_user_inserimento)
                                break
                            }
                        }
                    }
                }
            }
                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })
        }

    private fun mandaNotificaStudente(context:Context,id_user_inserimento: String?) {
        val studentePrenotazioneRef = database.child("studenti").child(id_user_inserimento.toString())
        studentePrenotazioneRef.addValueEventListener(object:ValueEventListener{
            override fun onDataChange(snapshotStud: DataSnapshot) {
                if(snapshotStud.exists()){
                    val studentePrenotazione = snapshotStud.getValue(Studente::class.java)
                    Log.d("studenteRetrievatoNome", studentePrenotazione?.nome.toString())
                    Log.d("studenteRetrivatoIddevice", studentePrenotazione?.id_device.toString())
                    val tokenStudentePrenotazione = studentePrenotazione?.id_device
                    Utils.inviaNotificaPushNoChat(context, tokenStudentePrenotazione.toString(),"Eliminata prenotazione", "Ci scusiamo ma una tua prenotazione è stata eliminata per poter inserire una lezione")

                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })

    }
}