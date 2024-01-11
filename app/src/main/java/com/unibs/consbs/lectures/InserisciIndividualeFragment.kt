package com.unibs.consbs.lectures

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.unibs.consbs.DBHelper
import com.unibs.consbs.R
import com.unibs.consbs.bookings.PrenotazioniActivity
import com.unibs.consbs.classrooms.PopupActivity
import com.unibs.consbs.data.Aula
import com.unibs.consbs.data.Evento
import com.unibs.consbs.data.Materia
import com.unibs.consbs.data.Studente
import com.unibs.consbs.databinding.FragmentLezioneIndividualeBinding
import com.unibs.consbs.utils.Utils
import java.text.SimpleDateFormat
import java.util.*

class InserisciIndividualeFragment : Fragment()  {
    lateinit var binding: FragmentLezioneIndividualeBinding
    lateinit var mAuth: FirebaseAuth
    lateinit var mDbRef: DatabaseReference
    lateinit var sharedPreferences : SharedPreferences
    lateinit var id_aula: String
    lateinit var id_materia: String
    lateinit var nome_aula: String
    //in confronto con altri utenti
    var prenotazioneNonDisponibile : Boolean = false
    //tra eventi dello stesso utente
    var prenotazioneInConflitto: Boolean = false
    lateinit var db : DBHelper
    private var timestampInizioPrenotz : Long = 0
    private var timestampFinePrenotz : Long = 0
    private var materia_selezionata: String =""
    private var isLezione:Boolean = false
    private var prenotazioneOdierna: Boolean = false
    private lateinit var context:Context
    private var tokenStudentePrenotazione: String? =""



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        id_aula = arguments?.getString("id_aula").toString()
        nome_aula = arguments?.getString("nome_aula").toString()
        context = requireContext()
        binding = FragmentLezioneIndividualeBinding.inflate(inflater, container, false)
        return binding.root
    }




    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        mAuth = Firebase.auth
        mDbRef = Firebase.database.reference
        val currentUser = mAuth.currentUser


        sharedPreferences = context.getSharedPreferences("preferences", Context.MODE_PRIVATE)
        val storedArrayString = sharedPreferences.getString("idMaterieList", "")
        val idMaterieList = storedArrayString?.split(",")?.toTypedArray() ?: emptyArray()


        val auleList= mutableMapOf<String,String>()
        val materieList = mutableMapOf<String,String>()

        val options = mutableListOf<String>()
        val adapter = ArrayAdapter(context, R.layout.spinner_list, options)
        adapter.setDropDownViewResource(R.layout.spinner_list)

        val optionsAula = mutableListOf<String>()
        val adapterAula = ArrayAdapter(context, R.layout.spinner_list, optionsAula)
        adapterAula.setDropDownViewResource(R.layout.spinner_list)


        //se arrivo da un'aula di AULE allora non ho bisogno di selezionare l'aula, altrimenti sì
        //Log.d("id_aula",id_aula)
        if(id_aula != "0" || nome_aula == null){
            binding.linearSceltaAula.visibility=View.GONE
        }else{
            //setto lo spinner aula
            mDbRef.child("aule").orderByChild("id").addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (snapshotAula in snapshot.children) {
                            val aula = snapshotAula.getValue(Aula::class.java)
                            Log.d("nomeAula", aula?.nome.toString())
                            if (aula != null) {
                                optionsAula.add(aula.nome.toString())
                                auleList.set(aula.nome.toString(), aula.id.toString())
                            }

                        }
                        //Log.d("auleList", auleList.size.toString())
                        adapterAula.notifyDataSetChanged()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.d("GET materia", "Errore: ${error.message}")
                    }

                })
        }




        //inseriamo le materie associate al prof in quelle che può selezionare da tendina
        for (idMateria in idMaterieList) {
            mDbRef.child("materie").child(idMateria)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {

                            val materia = snapshot.getValue(Materia::class.java)
                            if (materia != null) {
                                options.add(materia.nome.toString())
                                materieList.set(materia.nome.toString(),materia.id.toString())
                            }

                        }
                        //Log.d("materieList", materieList.size.toString())
                        adapter.notifyDataSetChanged()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.d("GET materia", "Errore: ${error.message}")
                    }

                })
        }


        val spinner: Spinner = view.findViewById(R.id.scelta_materia)
        val spinnerAula: Spinner = view.findViewById(R.id.scelta_aula_prenotazione)
        spinner.adapter = adapter
        spinnerAula.adapter=adapterAula



        //Retrievo la materia selezionata
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View, position: Int, id: Long
            ) {

                materia_selezionata = spinner.selectedItem.toString()
                val id_materia_selezionata = materieList.get(materia_selezionata)
                //Log.d("id_materia_selezionata_spinner", id_materia_selezionata.toString())
                id_materia= id_materia_selezionata.toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // write code to perform some action
            }

        }

        //Retrievo l'aula selezionata (se non sono arrivata da aule)
        if(id_aula.equals("0")) {
            spinnerAula.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View, position: Int, id: Long
                ) {
                    //Log.d("aula selezionata spinner", spinnerAula.selectedItem.toString())
                    val aula_selezionata = spinnerAula.selectedItem.toString()
                    val id_aula_selezionata = auleList.get(aula_selezionata)
                    //Log.d("id_aula_selezionata_spinner", id_aula_selezionata.toString())
                    id_aula=id_aula_selezionata.toString()
                    checkDisponibilita(id_aula.toInt())
                    nome_aula=aula_selezionata.toString()

                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // write code to perform some action
                }

            }
        }

        //Retrievo data, ora, durata della lezione selezionate

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
                context,
                { view, year, monthOfYear, dayOfMonth ->
                    val selectedDate = Calendar.getInstance()
                    selectedDate.set(year, monthOfYear, dayOfMonth)
                    val dayOfWeek = selectedDate.get(Calendar.DAY_OF_WEEK)
                    if(dayOfWeek!=Calendar.SUNDAY) {
                        binding.dataPrenotazione.setText(dayOfMonth.toString() + "/" + (monthOfYear + 1) + "/" + year)
                        checkDisponibilita(id_aula.toInt())
                    }else{
                        Toast.makeText(context, "Attenzione, Domenica il Conservatorio è chiuso",Toast.LENGTH_SHORT).show()
                    }
                },

                year,
                month,
                day,

                )
            datePickerDialog.datePicker.minDate=System.currentTimeMillis()
            datePickerDialog.datePicker.maxDate=System.currentTimeMillis()+168*60*60*1000
            datePickerDialog.show()
        }

        var formattedHour = if ( Utils.countDigits(hour) == 1 ) "0$hour" else hour
        var formattedMinute = if ( Utils.countDigits(minute) == 1 ) "0$minute" else minute
        binding.orarioPrenotazione.text = "$formattedHour:$formattedMinute"
        binding.orarioPrenotazione.setOnClickListener{
            val timePickerDialog = TimePickerDialog(
                context,
                { view, hourOfDay, minute ->
                    if(hourOfDay>=8 && hourOfDay<19 ){
                        formattedHour = if ( Utils.countDigits(hourOfDay) == 1 ) "0$hourOfDay" else hourOfDay
                        var f_minute = (minute/15)*15
                        formattedMinute = if ( Utils.countDigits(f_minute) == 1 ) "0$f_minute" else f_minute
                        binding.orarioPrenotazione.setText("$formattedHour:$formattedMinute")

                        checkDisponibilita(id_aula.toInt())
                    }
                    else{
                        Toast.makeText(context, "Attenzione, il Conservatorio è aperto dalle ore 8.00 alle ore 19.00",Toast.LENGTH_LONG).show()

                    }
                },
                hour,
                minute,
                true
            )

            timePickerDialog.show()

        }

        //check disponibilità al cambio di durata prenotazione
        binding.sceltaDurataPrenotazione.setOnCheckedChangeListener{ group, checkedId ->
            checkDisponibilita(id_aula.toInt())
        }

        mDbRef=Firebase.database.reference


        //check visualizza disponibilita con popup
        binding.visualizzaDisponibilita.setOnClickListener{
            val intent = Intent(context, PopupActivity::class.java)
            intent.putExtra("id_aula",id_aula.toInt())
            intent.putExtra("nome_aula", nome_aula)
            intent.putExtra("data_prenotazione",binding.dataPrenotazione.text)
            startActivity(intent)

        }

        //controllo bottone salva disponibilita, bottone presente nella activity
        val activity = requireActivity() as? InserisciLezioneActivity
        activity?.binding?.salvaPrenotazione?.setOnClickListener {
            Log.d("cliccatosalvapreno", "cliccato salva prenotazione fragment")
            Log.d("prenotazioneNonDisponibile", prenotazioneNonDisponibile.toString())
            Log.d("isLezione", isLezione.toString())
            Log.d("prenotazioneOdierna", prenotazioneOdierna.toString())


            //se l'aula non è libera a causa di una prenotazione e il prof NON sta prenotando per oggi, allora può sovrascrivere
           if ( prenotazioneNonDisponibile && !isLezione && !prenotazioneOdierna) {

                //dialog in cui chiedo a professore se è d'accordo nel sovrascrivere una prenotazione o piu (arco temporale sua lezione)
                val dialogBuilder = AlertDialog.Builder(context)

                val durataIdPrenotazione = binding.sceltaDurataPrenotazione.checkedRadioButtonId
                val durataPrenotazione = requireView().findViewById<RadioButton>(durataIdPrenotazione).text
                val dataPrenotazione = binding.dataPrenotazione.text
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm")
                val orarioPrenotazione = binding.orarioPrenotazione.text
                val dataOraInizioText = dataPrenotazione.toString()+" "+orarioPrenotazione.toString()
                val messageBox = "La tua lezione sovrascrive la prenotazione di uno studente. \n\n - Data: ${binding.dataPrenotazione.text}\n - Orario: ${binding.orarioPrenotazione.text}\n - Durata: ${durataPrenotazione}\n -  Aula:$nome_aula\n\nVuoi procedere e confermare la lezione?"

                dialogBuilder.setMessage(messageBox)
                    .setCancelable(false)
                    .setPositiveButton("Conferma sovrascrizione", DialogInterface.OnClickListener { dialog, id ->
                        db = DBHelper()
                        db.rimuovi_eventi_arcoTemporale(context,timestampInizioPrenotz, timestampFinePrenotz, id_aula, currentUser?.uid.toString(), dataOraInizioText)
                        db.aggiungi_evento(
                            "id_evento",
                            "Lezione - $materia_selezionata",
                            currentUser?.uid,
                            id_aula?.toInt(),
                            timestampInizioPrenotz.toDouble(),
                            timestampFinePrenotz.toDouble(),
                            false,
                            "",
                            id_materia
                        )

                        Toast.makeText(context, "Lezione confermata!", Toast.LENGTH_LONG).show()
                        val intent = Intent(context, PrenotazioniActivity::class.java)
                        startActivity(intent)
                        //requireActivity().finish()
                    })
                    .setNegativeButton("Annulla", DialogInterface.OnClickListener {
                            dialog, id -> dialog.cancel()
                    })

                val alert = dialogBuilder.create()

                alert.setTitle("Conferma prenotazione lezione")

                alert.show()

            //se l'aula non è libera a causa di una prenotazione e il prof sta prenotando per oggi, allora NON può sovrascrivere
            } else if(prenotazioneNonDisponibile && !isLezione && prenotazioneOdierna){
                Toast.makeText(context, "Attenzione! Non puoi sovrascrivere una prenotazione odierna", Toast.LENGTH_SHORT).show()

            //se l'aula non è libera a causa di una lezione allora NON può sovrascrivere
            }else if(prenotazioneNonDisponibile && isLezione){
                Toast.makeText(context, "Attenzione! Non puoi sovrascrivere delle lezioni", Toast.LENGTH_SHORT).show()

            //se il prof ha già una prenotazione per quel giorno stessa fascia oraria, allora NON può sovrascrivere
            }else if ( prenotazioneInConflitto ) {
                Toast.makeText(context, "Attenzione! Hai già effettuato una prenotazione per questa fascia oraria", Toast.LENGTH_SHORT).show()

            //se l'aula è disponibile e non ci sono conflitti, allora prenota
            } else {
                val dialogBuilder = AlertDialog.Builder(context)

                val durataIdPrenotazione = binding.sceltaDurataPrenotazione.checkedRadioButtonId
                val durataPrenotazione = requireView().findViewById<RadioButton>(durataIdPrenotazione).text
                val messageBox = "Riepilogo prenotazione lezione\n\n - Data: ${binding.dataPrenotazione.text}\n - Orario: ${binding.orarioPrenotazione.text}\n - Durata: ${durataPrenotazione}\n - Aula: ${nome_aula}\n\nVuoi confermare la prenotazione?"

                dialogBuilder.setMessage(messageBox)
                    .setCancelable(false)
                    .setPositiveButton("Conferma", DialogInterface.OnClickListener { dialog, id ->
                        db = DBHelper()
                        db.aggiungi_evento(
                            "id_evento",
                            "Lezione - $materia_selezionata",
                            currentUser?.uid,
                            id_aula?.toInt(),
                            timestampInizioPrenotz.toDouble(),
                            timestampFinePrenotz.toDouble(),
                            false,
                            "",
                            id_materia
                        )

                        Toast.makeText(context, "Lezione confermata!", Toast.LENGTH_LONG).show()
                        val intent = Intent(context, PrenotazioniActivity::class.java)
                        startActivity(intent)
                        requireActivity().finish()
                    })
                    .setNegativeButton("Annulla", DialogInterface.OnClickListener {
                            dialog, id -> dialog.cancel()
                    })

                val alert = dialogBuilder.create()

                alert.setTitle("Conferma prenotazione Lezione")

                alert.show()
            }
        }

        checkDisponibilita(id_aula.toInt())



        }







    private fun checkDisponibilita(id_aula: Int) {
        val timestampOggi = System.currentTimeMillis()
        val dataOggi = Utils.timestampToFormatDate(timestampOggi.toLong(), "dd/MM/yyyy")
        val dataPrenotazione = binding.dataPrenotazione.text
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm")
        val orarioPrenotazione = binding.orarioPrenotazione.text
        val dataOraInizioText = dataPrenotazione.toString()+" "+orarioPrenotazione.toString()
        val dataOraInizio = dateFormat.parse(dataOraInizioText.toString())
        timestampInizioPrenotz = Utils.formatDateToTimestamp(dataOraInizioText)
        val durataIdPrenotazione = binding.sceltaDurataPrenotazione.checkedRadioButtonId
        val durataPrenotazione = requireView().findViewById<RadioButton>(durataIdPrenotazione).text
        val durataPrenotazioneHashmap = HashMap<String, Long>()
        durataPrenotazioneHashmap["30 min"] = 30*60*1000
        durataPrenotazioneHashmap["1 ora"] = 1*60*60*1000
        durataPrenotazioneHashmap["2 ore"] = 2*60*60*1000
        timestampFinePrenotz = timestampInizioPrenotz + durataPrenotazioneHashmap[durataPrenotazione.toString()]!!
        //prenotazioneNonDisponibile = false

        val MSG_ORARIO_PASSATO = "Attenzione! Non è possibile prenotare per una data e/o un orario precedenti a quelli attuali"
        val MSG_CONFLITTO_FASCIAORARIA = "Attenzione! Hai già una prenotazione per questa fascia oraria"
        val MSG_FUORI_RANGE = "Attenzione! Non è possibile prenotare un'aula oltre le 19:00"
        val MSG_CONFLITTO_ALTRI_EVENTI_SOVRASCRIVI = "Attenzione! E' presente una prenotazione di uno studente. Puoi sovrascrivere"
        val MSG_CONFLITTO_ALTRI_EVENTI_LEZIONE = "Attenzione! Non puoi prenotare perchè è già presente una lezione"
        val MSG_CONFLITTO_ALTRI_EVENTI_PRENOTAZIONE = "Attenzione! Non puoi prenotare perchè è già presente una prenotazione non sovrascrivibile"
        val MSG_DISPONIBILE = "L'aula è disponibile"
        val COL_NON_DISPONIBILE = android.R.color.holo_red_light
        val COL_SOVRASCRIVI = android.R.color.holo_orange_dark
        val COL_DISPONIBILE = android.R.color.holo_green_light


        // Controllo Range Orari
        if ( timestampInizioPrenotz < timestampOggi ) {
            binding.textMsgDisponibilita.text = MSG_ORARIO_PASSATO
            binding.textMsgDisponibilita.setTextColor(ContextCompat.getColor(context, COL_NON_DISPONIBILE))
            binding.textMsgDisponibilita.visibility = View.VISIBLE
            prenotazioneNonDisponibile = true
        }
        var dataInizioPrenotz = Utils.timestampToFormatDate(timestampInizioPrenotz, "dd/MM/yyyy")
        var dataFinePrenotz = Utils.timestampToDate(timestampFinePrenotz)
        if ( dataFinePrenotz.hours > 19 || ( dataFinePrenotz.hours == 19 && dataFinePrenotz.minutes != 0 ) ) {
            binding.textMsgDisponibilita.text = MSG_FUORI_RANGE
            binding.textMsgDisponibilita.setTextColor(ContextCompat.getColor(context, COL_NON_DISPONIBILE))
            binding.textMsgDisponibilita.visibility = View.VISIBLE
            prenotazioneNonDisponibile = true
        }

        val eventiRef = mDbRef.child("eventi").orderByChild("id_aula").equalTo(id_aula!!.toDouble())

        eventiRef.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (eventoSnapshot in snapshot.children) {
                    val evento = eventoSnapshot.getValue(Evento::class.java)
                    isLezione = evento?.prenotazione == false


                    // Controllo Conflitti con lezioni o prenotazioni GIA PRESENTI
                    if(Utils.checkTimestampDay(evento!!.data_ora_inizio.toLong(), dateFormat.parse(dataOraInizioText.toString()))) {

                        if (timestampInizioPrenotz >= evento.data_ora_inizio && timestampFinePrenotz <= evento.data_ora_fine) {
                            //la prenotazione dell'utente è parzialmente o interamente contenuta nel range di tempo dell'evento

                            //se è già presente una lezione, msg che non si può sovrascrivere
                            if(isLezione){
                                binding.textMsgDisponibilita.text = MSG_CONFLITTO_ALTRI_EVENTI_LEZIONE
                                binding.textMsgDisponibilita.setTextColor(
                                    ContextCompat.getColor(
                                        context,
                                        COL_NON_DISPONIBILE
                                    )
                                )
                                prenotazioneOdierna=false

                            //se è già presente una prenotazione ed è per oggi, msg che non si può sovrascrivere
                            }else if(!isLezione && dataOggi==dataInizioPrenotz){

                                binding.textMsgDisponibilita.text = MSG_CONFLITTO_ALTRI_EVENTI_PRENOTAZIONE
                                binding.textMsgDisponibilita.setTextColor(
                                    ContextCompat.getColor(
                                        context,
                                        COL_NON_DISPONIBILE
                                    )
                                )
                                prenotazioneOdierna=true

                            //se è già presente una prenotazione e NON è per oggi, msg che si può sovrascrivere
                            } else if( !isLezione && dataOggi!=dataInizioPrenotz){
                                binding.textMsgDisponibilita.text = MSG_CONFLITTO_ALTRI_EVENTI_SOVRASCRIVI
                                binding.textMsgDisponibilita.setTextColor(
                                    ContextCompat.getColor(
                                        context,
                                        COL_SOVRASCRIVI
                                    )
                                )
                                prenotazioneOdierna=false
                            }

                            binding.textMsgDisponibilita.visibility = View.VISIBLE
                            prenotazioneNonDisponibile = true
                            break


                        } else if (timestampInizioPrenotz < evento.data_ora_inizio && timestampFinePrenotz <= evento.data_ora_fine && timestampFinePrenotz > evento.data_ora_inizio) {
                            //la prenotazione dell'utente inizia prima dell'evento e finisce durante o esattamente alla fine dell'evento

                            //se è già presente una lezione, msg che non si può sovrascrivere
                            if(isLezione){
                                binding.textMsgDisponibilita.text = MSG_CONFLITTO_ALTRI_EVENTI_LEZIONE
                                binding.textMsgDisponibilita.setTextColor(
                                    ContextCompat.getColor(
                                        context,
                                        COL_NON_DISPONIBILE
                                    )
                                )
                                prenotazioneOdierna=false

                                //se è già presente una prenotazione ed è per oggi, msg che non si può sovrascrivere
                            }else if(!isLezione && dataOggi==dataInizioPrenotz){
                                binding.textMsgDisponibilita.text = MSG_CONFLITTO_ALTRI_EVENTI_PRENOTAZIONE
                                binding.textMsgDisponibilita.setTextColor(
                                    ContextCompat.getColor(
                                        context,
                                        COL_NON_DISPONIBILE
                                    )
                                )
                                prenotazioneOdierna=true

                                //se è già presente una prenotazione e NON è per oggi, msg che si può sovrascrivere
                            } else if( !isLezione && dataOggi!=dataInizioPrenotz){
                                binding.textMsgDisponibilita.text = MSG_CONFLITTO_ALTRI_EVENTI_SOVRASCRIVI
                                binding.textMsgDisponibilita.setTextColor(
                                    ContextCompat.getColor(
                                        context,
                                        COL_SOVRASCRIVI
                                    )
                                )
                                prenotazioneOdierna=false
                            }

                            binding.textMsgDisponibilita.visibility = View.VISIBLE
                            prenotazioneNonDisponibile = true
                            break

                        }else if(timestampInizioPrenotz >= evento.data_ora_inizio && timestampInizioPrenotz < evento.data_ora_fine && timestampFinePrenotz > evento.data_ora_fine){
                            //la prenotazione dell'utente inizia esattamente all'inizio o durante l'evento, e finisce dopo la fine dell'evento
                            //se è già presente una lezione, msg che non si può sovrascrivere
                            if(isLezione){
                                binding.textMsgDisponibilita.text = MSG_CONFLITTO_ALTRI_EVENTI_LEZIONE
                                binding.textMsgDisponibilita.setTextColor(
                                    ContextCompat.getColor(
                                        context,
                                        COL_NON_DISPONIBILE
                                    )
                                )
                                prenotazioneOdierna=false

                                //se è già presente una prenotazione ed è per oggi, msg che non si può sovrascrivere
                            }else if(!isLezione && dataOggi==dataInizioPrenotz){
                                binding.textMsgDisponibilita.text = MSG_CONFLITTO_ALTRI_EVENTI_PRENOTAZIONE
                                binding.textMsgDisponibilita.setTextColor(
                                    ContextCompat.getColor(
                                        context,
                                        COL_NON_DISPONIBILE
                                    )
                                )
                                prenotazioneOdierna=true

                                //se è già presente una prenotazione e NON è per oggi, msg che si può sovrascrivere
                            } else if( !isLezione && dataOggi!=dataInizioPrenotz){
                                binding.textMsgDisponibilita.text = MSG_CONFLITTO_ALTRI_EVENTI_SOVRASCRIVI
                                binding.textMsgDisponibilita.setTextColor(
                                    ContextCompat.getColor(
                                        context,
                                        COL_SOVRASCRIVI
                                    )
                                )
                                prenotazioneOdierna=false
                            }

                            binding.textMsgDisponibilita.visibility = View.VISIBLE
                            prenotazioneNonDisponibile = true
                            break
                        } else if (timestampInizioPrenotz < evento.data_ora_inizio && timestampFinePrenotz > evento.data_ora_fine) {
                            // la orenotazione ingloba un evento

                            //se è già presente una lezione, msg che non si può sovrascrivere
                            if(isLezione){
                                binding.textMsgDisponibilita.text = MSG_CONFLITTO_ALTRI_EVENTI_LEZIONE
                                binding.textMsgDisponibilita.setTextColor(
                                    ContextCompat.getColor(
                                        context,
                                        COL_NON_DISPONIBILE
                                    )
                                )
                                prenotazioneOdierna=false

                                //se è già presente una prenotazione ed è per oggi, msg che non si può sovrascrivere
                            }else if(!isLezione && dataOggi==dataInizioPrenotz){
                                binding.textMsgDisponibilita.text = MSG_CONFLITTO_ALTRI_EVENTI_PRENOTAZIONE
                                binding.textMsgDisponibilita.setTextColor(
                                    ContextCompat.getColor(
                                        context,
                                        COL_NON_DISPONIBILE
                                    )
                                )
                                prenotazioneOdierna=true

                                //se è già presente una prenotazione e NON è per oggi, msg che si può sovrascrivere
                            } else if( !isLezione && dataOggi!=dataInizioPrenotz){
                                binding.textMsgDisponibilita.text = MSG_CONFLITTO_ALTRI_EVENTI_SOVRASCRIVI
                                binding.textMsgDisponibilita.setTextColor(
                                    ContextCompat.getColor(
                                        context,
                                        COL_SOVRASCRIVI
                                    )
                                )
                                prenotazioneOdierna=false
                            }

                            binding.textMsgDisponibilita.visibility = View.VISIBLE
                            prenotazioneNonDisponibile = true
                            break
                        }
                    }
                }
                //Prenotazione è fattibile
                if (prenotazioneNonDisponibile == false) {
                    binding.textMsgDisponibilita.text = MSG_DISPONIBILE
                    binding.textMsgDisponibilita.setTextColor(ContextCompat.getColor(context, COL_DISPONIBILE))
                    binding.textMsgDisponibilita.visibility= View.VISIBLE
                }
            }


            override fun onCancelled(error: DatabaseError) {
                Log.e("GET studenti-chat", "Errore: ${error.message}")
            }
        })

        prenotazioneInConflitto = false

        val eventiMieiRef = mDbRef.child("eventi").orderByChild("id_user_inserimento").equalTo(mAuth.currentUser?.uid.toString())

        eventiMieiRef.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for ( eventoSnapshot in snapshot.children ) {
                    val evento = eventoSnapshot.getValue(Evento::class.java)

                    //Controllo che il prof non abbia già prenotato per lo stesso giorno e stessa fascia oraria
                    if ( evento != null ) {
                        if (timestampInizioPrenotz >= evento.data_ora_inizio && timestampFinePrenotz <= evento.data_ora_fine) {
                            //la prenotazione dell'utente è parzialmente o interamente contenuta nel range di tempo dell'evento
                            binding.textMsgDisponibilita.text = MSG_CONFLITTO_FASCIAORARIA
                            binding.textMsgDisponibilita.setTextColor(ContextCompat.getColor(context, COL_NON_DISPONIBILE))
                            binding.textMsgDisponibilita.visibility= View.VISIBLE
                            prenotazioneInConflitto = true
                            break
                        } else if (timestampInizioPrenotz<evento.data_ora_inizio && timestampFinePrenotz<=evento.data_ora_fine && timestampFinePrenotz > evento.data_ora_inizio) {
                            //la prenotazione dell'utente inizia prima dell'evento e finisce durante o esattamente alla fine dell'evento
                            binding.textMsgDisponibilita.text = MSG_CONFLITTO_FASCIAORARIA
                            binding.textMsgDisponibilita.setTextColor(ContextCompat.getColor(context, COL_NON_DISPONIBILE))
                            binding.textMsgDisponibilita.visibility= View.VISIBLE
                            prenotazioneInConflitto = true
                            break
                        } else if(timestampInizioPrenotz>=evento.data_ora_inizio && timestampInizioPrenotz< evento.data_ora_fine && timestampFinePrenotz>evento.data_ora_fine){
                            //la prenotazione dell'utente inizia esattamente all'inizio o durante l'evento, e finisce dopo la fine dell'evento
                            binding.textMsgDisponibilita.text = MSG_CONFLITTO_FASCIAORARIA
                            binding.textMsgDisponibilita.setTextColor(ContextCompat.getColor(context, COL_NON_DISPONIBILE))
                            binding.textMsgDisponibilita.visibility= View.VISIBLE
                            prenotazioneInConflitto = true
                            break
                        } else if ( timestampInizioPrenotz < evento.data_ora_inizio && timestampFinePrenotz > evento.data_ora_fine ) {
                            // la orenotazione ingloba un evento
                            binding.textMsgDisponibilita.text = MSG_CONFLITTO_FASCIAORARIA
                            binding.textMsgDisponibilita.setTextColor(ContextCompat.getColor(context, COL_NON_DISPONIBILE))
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





