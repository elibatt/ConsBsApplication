package com.unibs.consbs.classrooms

import android.R
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.unibs.consbs.BaseActivity
import com.unibs.consbs.DBHelper
import com.unibs.consbs.bookings.PrenotazioniActivity
import com.unibs.consbs.data.Evento
import com.unibs.consbs.databinding.PrenotazioneAulaBinding

import com.unibs.consbs.utils.FirebaseUtils
import com.unibs.consbs.utils.Utils
import java.text.SimpleDateFormat
import java.util.*

class PrenotazioneAulaActivity: BaseActivity() {
    lateinit var binding: PrenotazioneAulaBinding
    private lateinit var mDbRef: DatabaseReference
    //in confronto con altri utenti
    var prenotazioneNonDisponibile : Boolean = false
    //tra eventi dello stesso utente
    var prenotazioneInConflitto: Boolean = false
    lateinit var db : DBHelper
    private var timestampInizioPrenotz : Long = 0
    private var timestampFinePrenotz : Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {

        mAuth = Firebase.auth
        val currentUser = mAuth.currentUser

        super.onCreate(savedInstanceState)
        binding = PrenotazioneAulaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Utils.setCurrentActivty("PrenotazioneAulaActivity")

        FirebaseUtils.checkUserLoggedIn(this)

        binding.menu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        // Menù
        setNavigationViewSelectedItem("Aule")
        setOptionsItemsListener()

        val nome_aula=intent.getStringExtra("nome_aula")
        val nome_sede=intent.getStringExtra("nome_sede")
        val nome_piano=intent.getStringExtra("nome_piano")
        val indirizzo=intent.getStringExtra("indirizzo")
        val id_aula=intent.getIntExtra("id_aula",0)
        binding.textAulaNome.text=nome_aula
        binding.textSede.text=nome_sede
        binding.textIndirizzo.text=indirizzo
        binding.textPiano.text=nome_piano

        var c = Calendar.getInstance()
        var c_hour = c.get(Calendar.HOUR_OF_DAY)
        var c_minute = c.get(Calendar.MINUTE)

        var (hour, minute) = Utils.formatTimeQuarter(c_hour, c_minute)

        // giorno predefinito: verifica se si trova entro il range
        if ( hour < 8 ) {
            hour = 8
            minute = 0
        } else if ( hour >= 19 ) {
            hour = 8
            minute = 0
            c.add(Calendar.DAY_OF_YEAR, 1)
        }

        val dayOfWeek = c.get(Calendar.DAY_OF_WEEK)
        if ( dayOfWeek == Calendar.SUNDAY ) {
            c.add(Calendar.DAY_OF_YEAR, 1)
        }

        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        binding.dataPrenotazione.text="$day/${month + 1}/$year"
        binding.dataPrenotazione.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                this,
                { view, year, monthOfYear, dayOfMonth ->
                    val selectedDate = Calendar.getInstance()
                    selectedDate.set(year, monthOfYear, dayOfMonth)
                    val dayOfWeek = selectedDate.get(Calendar.DAY_OF_WEEK)
                    if(dayOfWeek!=Calendar.SUNDAY) {
                        binding.dataPrenotazione.setText(dayOfMonth.toString() + "/" + (monthOfYear + 1) + "/" + year)

                        checkDisponibilita(id_aula)
                    }else{
                        Toast.makeText(this@PrenotazioneAulaActivity, "Attenzione, Domenica il Conservatorio è chiuso",Toast.LENGTH_SHORT).show()
                    }
                },

                year,
                month,
                day,

            )
            datePickerDialog.datePicker.minDate=System.currentTimeMillis()
            datePickerDialog.datePicker.maxDate=System.currentTimeMillis()+72*60*60*1000
            datePickerDialog.show()
        }

        var formattedHour = if ( Utils.countDigits(hour) == 1 ) "0$hour" else hour
        var formattedMinute = if ( Utils.countDigits(minute) == 1 ) "0$minute" else minute
        binding.orarioPrenotazione.text = "$formattedHour:$formattedMinute"
        binding.orarioPrenotazione.setOnClickListener{
            val timePickerDialog = TimePickerDialog(
                this,
                { view, hourOfDay, minute ->
                    if(hourOfDay>=8 && hourOfDay<=19 ){
                        formattedHour = if ( Utils.countDigits(hourOfDay) == 1 ) "0$hourOfDay" else hourOfDay
                        var f_minute = (minute/15)*15
                        formattedMinute = if ( Utils.countDigits(f_minute) == 1 ) "0$f_minute" else f_minute
                        binding.orarioPrenotazione.setText("$formattedHour:$formattedMinute")

                        checkDisponibilita(id_aula)
                    }
                    else{
                        Toast.makeText(this@PrenotazioneAulaActivity, "Attenzione, il Conservatorio è aperto dalle ore 8.00 alle ore 19.00",Toast.LENGTH_LONG).show()
                    }
                },
                hour,
                minute,
                true
            )

            timePickerDialog.show()

        }

        binding.sceltaDurataPrenotazione.setOnCheckedChangeListener{ group, checkedId ->
            checkDisponibilita(id_aula)
        }

        mDbRef=Firebase.database.reference
        val eventiRef = mDbRef.child("eventi").orderByChild("id_aula").equalTo(id_aula!!.toDouble())
        var uscitaFor = false
        eventiRef.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (eventoSnapshot in snapshot.children) {
                    val evento = eventoSnapshot.getValue(Evento::class.java)
                    val timestampoggi = System.currentTimeMillis().toDouble()

                    if (Utils.isTimestampToday(evento!!.data_ora_inizio.toLong())) {
                        if (evento.data_ora_inizio <= timestampoggi && timestampoggi <= evento.data_ora_fine) {
                            binding.textStatoattuale.text= "Stato attuale: 🔴"
                            uscitaFor = true
                            break
                        } else if (evento.data_ora_inizio - 30 * 60 * 1000.0 <= timestampoggi && timestampoggi <= evento.data_ora_fine) {
                            binding.textStatoattuale.text = "Stato attuale: 🟠"
                            uscitaFor = true
                            break
                        }
                    }
                }
                if (uscitaFor == false)   binding.textStatoattuale.text= "Stato attuale: 🟢"
            }


            override fun onCancelled(error: DatabaseError) {
                Log.e("GET studenti-chat", "Errore: ${error.message}")
            }
        })

        binding.visualizzaDisponibilita.setOnClickListener{
            val intent = Intent(this@PrenotazioneAulaActivity, PopupActivity::class.java)
            intent.putExtra("id_aula",id_aula)
            intent.putExtra("nome_aula", nome_aula)
            intent.putExtra("data_prenotazione",binding.dataPrenotazione.text)
            startActivity(intent)

        }

        binding.salvaPrenotazione.setOnClickListener{
            if ( prenotazioneNonDisponibile ) {
                Toast.makeText(this, "Attenzione! Ricontrollare i dati della prenotazione", Toast.LENGTH_SHORT).show()
            } else if ( prenotazioneInConflitto ) {
                Toast.makeText(this, "Attenzione! Hai già effettuato una prenotazione per questa fascia oraria", Toast.LENGTH_SHORT).show()
            } else {
                val dialogBuilder = AlertDialog.Builder(this)

                val durataIdPrenotazione = binding.sceltaDurataPrenotazione.checkedRadioButtonId
                val durataPrenotazione = findViewById<RadioButton>(durataIdPrenotazione).text
                val messageBox = "Riepilogo Prenotazione\n\n - Data: ${binding.dataPrenotazione.text}\n - Orario: ${binding.orarioPrenotazione.text}\n - Durata: ${durataPrenotazione}\n - Aula: $nome_aula\n\nVuoi confermare la prenotazione?"

                dialogBuilder.setMessage(messageBox)
                    .setCancelable(false)
                    .setPositiveButton("Conferma", DialogInterface.OnClickListener { dialog, id ->
                        db = DBHelper()
                        db.aggiungi_evento(
                            "id_evento",
                            "Prenotazione - $nome_aula",
                            currentUser?.uid,
                            id_aula,
                            timestampInizioPrenotz.toDouble(),
                            timestampFinePrenotz.toDouble(),
                            true,
                            "",
                            ""
                        )

                        Toast.makeText(this, "Prenotazione confermata!", Toast.LENGTH_LONG).show()
                        val intent = Intent(this@PrenotazioneAulaActivity, PrenotazioniActivity::class.java)
                        startActivity(intent)
                        finish()
                    })
                    .setNegativeButton("Annulla", DialogInterface.OnClickListener {
                            dialog, id -> dialog.cancel()
                    })

                val alert = dialogBuilder.create()

                alert.setTitle("Conferma Prenotazione")

                alert.show()
            }
        }

        checkDisponibilita(id_aula)

    }


    private fun checkDisponibilita(id_aula: Int) {
        val timestampOggi = System.currentTimeMillis()

        val dataPrenotazione = binding.dataPrenotazione.text
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm")
        val orarioPrenotazione = binding.orarioPrenotazione.text
        val dataOraInizioText = dataPrenotazione.toString()+" "+orarioPrenotazione.toString()
        val dataOraInizio = dateFormat.parse(dataOraInizioText.toString())
        timestampInizioPrenotz = Utils.formatDateToTimestamp(dataOraInizioText)
        val durataIdPrenotazione = binding.sceltaDurataPrenotazione.checkedRadioButtonId
        val durataPrenotazione = findViewById<RadioButton>(durataIdPrenotazione).text
        val durataPrenotazioneHashmap = HashMap<String, Long>()
        durataPrenotazioneHashmap["30 min"] = 30*60*1000
        durataPrenotazioneHashmap["1 ora"] = 1*60*60*1000
        durataPrenotazioneHashmap["2 ore"] = 2*60*60*1000
        timestampFinePrenotz = timestampInizioPrenotz + durataPrenotazioneHashmap[durataPrenotazione.toString()]!!
        prenotazioneNonDisponibile = false

        val MSG_ORARIO_PASSATO = "Attenzione! Non è possibile prenotare per una data e/o un orario precedenti a quelli attuali"
        val MSG_CONFLITTO = "L'aula non è disponibile per l'orario e la durata inseriti"
        val MSG_FUORI_RANGE = "Attenzione! Non è possibile prenotare un'aula oltre le 19:00"
        val MSG_CONFLITTO_ALTRI_EVENTI = "Attenzione! Hai già effettuato una prenotazione per questa fascia oraria"
        val COL_NON_DISPONIBILE = R.color.holo_red_light

        val MSG_DISPONIBILE = "È disponibile l'aula"
        val COL_DISPONIBILE = R.color.holo_green_light


        // Controllo Range Orari
        if ( timestampInizioPrenotz < timestampOggi ) {
            binding.textMsgDisponibilita.text = MSG_ORARIO_PASSATO
            binding.textMsgDisponibilita.setTextColor(ContextCompat.getColor(this@PrenotazioneAulaActivity, COL_NON_DISPONIBILE))
            binding.textMsgDisponibilita.visibility = View.VISIBLE
            prenotazioneNonDisponibile = true
        }

        var dataFinePrenotz = Utils.timestampToDate(timestampFinePrenotz)
        if ( dataFinePrenotz.hours > 19 || ( dataFinePrenotz.hours == 19 && dataFinePrenotz.minutes != 0 ) ) {
            binding.textMsgDisponibilita.text = MSG_FUORI_RANGE
            binding.textMsgDisponibilita.setTextColor(ContextCompat.getColor(this@PrenotazioneAulaActivity, COL_NON_DISPONIBILE))
            binding.textMsgDisponibilita.visibility = View.VISIBLE
            prenotazioneNonDisponibile = true
        }

        val eventiRef = mDbRef.child("eventi").orderByChild("id_aula").equalTo(id_aula!!.toDouble())

        eventiRef.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (eventoSnapshot in snapshot.children) {
                    val evento = eventoSnapshot.getValue(Evento::class.java)

                    // Controllo Conflitti con Eventi
                    if(Utils.checkTimestampDay(evento!!.data_ora_inizio.toLong(), dateFormat.parse(dataOraInizioText.toString()))) {
                        if (timestampInizioPrenotz >= evento.data_ora_inizio && timestampFinePrenotz <= evento.data_ora_fine) {
                            //la prenotazione dell'utente è parzialmente o interamente contenuta nel range di tempo dell'evento
                            binding.textMsgDisponibilita.text = MSG_CONFLITTO
                            binding.textMsgDisponibilita.setTextColor(
                                ContextCompat.getColor(
                                    this@PrenotazioneAulaActivity,
                                    COL_NON_DISPONIBILE
                                )
                            )
                            binding.textMsgDisponibilita.visibility = View.VISIBLE
                            prenotazioneNonDisponibile = true
                            break
                        } else if (timestampInizioPrenotz < evento.data_ora_inizio && timestampFinePrenotz <= evento.data_ora_fine && timestampFinePrenotz > evento.data_ora_inizio) {
                            //la prenotazione dell'utente inizia prima dell'evento e finisce durante o esattamente alla fine dell'evento
                            binding.textMsgDisponibilita.text = MSG_CONFLITTO
                            binding.textMsgDisponibilita.setTextColor(
                                ContextCompat.getColor(
                                    this@PrenotazioneAulaActivity,
                                    COL_NON_DISPONIBILE
                                )
                            )
                            binding.textMsgDisponibilita.visibility = View.VISIBLE
                            prenotazioneNonDisponibile = true
                            break
                        } else if (timestampInizioPrenotz >= evento.data_ora_inizio && timestampInizioPrenotz < evento.data_ora_fine && timestampFinePrenotz > evento.data_ora_fine) {
                            //la prenotazione dell'utente inizia esattamente all'inizio o durante l'evento, e finisce dopo la fine dell'evento
                            binding.textMsgDisponibilita.text = MSG_CONFLITTO
                            binding.textMsgDisponibilita.setTextColor(
                                ContextCompat.getColor(
                                    this@PrenotazioneAulaActivity,
                                    COL_NON_DISPONIBILE
                                )
                            )
                            binding.textMsgDisponibilita.visibility = View.VISIBLE
                            prenotazioneNonDisponibile = true
                            break
                        } else if (timestampInizioPrenotz < evento.data_ora_inizio && timestampFinePrenotz > evento.data_ora_fine) {
                            // la orenotazione ingloba un evento
                            binding.textMsgDisponibilita.text = MSG_CONFLITTO
                            binding.textMsgDisponibilita.setTextColor(
                                ContextCompat.getColor(
                                    this@PrenotazioneAulaActivity,
                                    COL_NON_DISPONIBILE
                                )
                            )
                            binding.textMsgDisponibilita.visibility = View.VISIBLE
                            prenotazioneNonDisponibile = true
                            break
                        }
                    }
                }
                //Prenotazione è fattibile
                if (prenotazioneNonDisponibile == false) {
                    binding.textMsgDisponibilita.text = MSG_DISPONIBILE
                    binding.textMsgDisponibilita.setTextColor(ContextCompat.getColor(this@PrenotazioneAulaActivity, COL_DISPONIBILE))
                    binding.textMsgDisponibilita.visibility= View.VISIBLE
                }
            }


            override fun onCancelled(error: DatabaseError) {
                Log.e("GET studenti-chat", "Errore: ${error.message}")
            }
        })

        prenotazioneInConflitto = false

        val eventiMieiRef = mDbRef.child("eventi").orderByChild("id_user_inserimento").equalTo(mAuth.currentUser?.uid.toString())

        eventiMieiRef.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for ( eventoSnapshot in snapshot.children ) {
                    val evento = eventoSnapshot.getValue(Evento::class.java)

                    if ( evento != null ) {
                        if (timestampInizioPrenotz >= evento.data_ora_inizio && timestampFinePrenotz <= evento.data_ora_fine) {
                            //la prenotazione dell'utente è parzialmente o interamente contenuta nel range di tempo dell'evento
                            binding.textMsgDisponibilita.text = MSG_CONFLITTO_ALTRI_EVENTI
                            binding.textMsgDisponibilita.setTextColor(ContextCompat.getColor(this@PrenotazioneAulaActivity, COL_NON_DISPONIBILE))
                            binding.textMsgDisponibilita.visibility= View.VISIBLE
                            prenotazioneInConflitto = true
                            break
                        } else if (timestampInizioPrenotz<evento.data_ora_inizio && timestampFinePrenotz<=evento.data_ora_fine && timestampFinePrenotz > evento.data_ora_inizio) {
                            //la prenotazione dell'utente inizia prima dell'evento e finisce durante o esattamente alla fine dell'evento
                            binding.textMsgDisponibilita.text = MSG_CONFLITTO_ALTRI_EVENTI
                            binding.textMsgDisponibilita.setTextColor(ContextCompat.getColor(this@PrenotazioneAulaActivity, COL_NON_DISPONIBILE))
                            binding.textMsgDisponibilita.visibility= View.VISIBLE
                            prenotazioneInConflitto = true
                            break
                        } else if(timestampInizioPrenotz>=evento.data_ora_inizio && timestampInizioPrenotz< evento.data_ora_fine && timestampFinePrenotz>evento.data_ora_fine){
                            //la prenotazione dell'utente inizia esattamente all'inizio o durante l'evento, e finisce dopo la fine dell'evento
                            binding.textMsgDisponibilita.text = MSG_CONFLITTO_ALTRI_EVENTI
                            binding.textMsgDisponibilita.setTextColor(ContextCompat.getColor(this@PrenotazioneAulaActivity, COL_NON_DISPONIBILE))
                            binding.textMsgDisponibilita.visibility= View.VISIBLE
                            prenotazioneInConflitto = true
                            break
                        } else if ( timestampInizioPrenotz < evento.data_ora_inizio && timestampFinePrenotz > evento.data_ora_fine ) {
                            // la orenotazione ingloba un evento
                            binding.textMsgDisponibilita.text = MSG_CONFLITTO_ALTRI_EVENTI
                            binding.textMsgDisponibilita.setTextColor(ContextCompat.getColor(this@PrenotazioneAulaActivity, COL_NON_DISPONIBILE))
                            binding.textMsgDisponibilita.visibility= View.VISIBLE
                            prenotazioneInConflitto = true
                            break
                        }
                    }

                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("GET eventi miei", "Errore: ${error.message}")
            }

        })
    }

}
