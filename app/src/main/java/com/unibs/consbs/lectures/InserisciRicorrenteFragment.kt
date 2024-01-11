package com.unibs.consbs.lectures

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil.setContentView
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
import com.unibs.consbs.Professore_Materia
import com.unibs.consbs.R
import com.unibs.consbs.data.Evento
import com.unibs.consbs.data.Materia
import com.unibs.consbs.databinding.FragmentLezioneIndividualeBinding
import com.unibs.consbs.databinding.FragmentLezioneRicorrenteBinding
import com.unibs.consbs.databinding.PrenotazioniBinding
import com.unibs.consbs.utils.Utils
import java.text.SimpleDateFormat
import java.util.*

class InserisciRicorrenteFragment : Fragment()  {
    lateinit var binding: FragmentLezioneRicorrenteBinding
    lateinit var mAuth: FirebaseAuth
    lateinit var mDbRef: DatabaseReference
    lateinit var sharedPreferences : SharedPreferences
    lateinit var idAula:String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        idAula = arguments?.getString("id_aula").toString()
        binding = FragmentLezioneRicorrenteBinding.inflate(inflater, container, false)
        return binding.root
    }




    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mAuth = Firebase.auth
        mDbRef = Firebase.database.reference

        sharedPreferences =
            requireContext().getSharedPreferences("preferences", Context.MODE_PRIVATE)
        val storedArrayString = sharedPreferences.getString("idMaterieList", "")
        //val idMaterieList = storedArrayString?.split(",")?.toTypedArray() ?: emptyArray()
        val idMaterieList: List<String> = storedArrayString?.split(",") ?: emptyList()


        val options = mutableListOf<String>()
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_list, options)
        adapter.setDropDownViewResource(R.layout.spinner_list)

        /*
        mDbRef.child("professori_materie").orderByChild("id_professore").equalTo(mAuth.currentUser?.uid.toString()).addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if ( snapshot.exists() ) {
                    val idMaterieList = snapshot.children
                                             .mapNotNull { it.getValue(Professore_Materia::class.java) }
                                             .map { it.id_materia }

                    for ( idMateria in idMaterieList ) {
                        mDbRef.child("materie").child(idMateria.toString()).addValueEventListener(object: ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if ( snapshot.exists() ) {
                                    val materia = snapshot.getValue(Materia::class.java)
                                    if ( materia != null ) {
                                        options.add(materia.nome.toString())
                                    }

                                }
                                adapter.notifyDataSetChanged()
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.d("GET materia", "Errore: ${error.message}")
                            }

                        })
                    }

                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("GET professori_materie", "Errore: ${error.message}")
            }

        })
        */

        //se arrivo da un'aula di AULE allora non ho bisogno di selezionare l'aula, altrimenti si
        if(idAula!="0"){
            binding.linearSceltaAula.visibility=View.GONE
        }

        //inseriamo le materie associate al prof in quelle che può selezionare da tendina
        for (idMateria in idMaterieList) {
            Log.d("fragment materia", idMateria)
            mDbRef.child("materie").child(idMateria.toString())
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val materia = snapshot.getValue(Materia::class.java)
                            if (materia != null) {
                                options.add(materia.nome.toString())
                            }

                        }
                        adapter.notifyDataSetChanged()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.d("GET materia", "Errore: ${error.message}")
                    }

                })
        }

        val spinner: Spinner = view.findViewById(R.id.scelta_materia)

        spinner.adapter = adapter

        //Retrievo la materia selezionata
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View, position: Int, id: Long
            ) {
                Log.d("selezionato", spinner.selectedItem.toString())
                val materia_selezionata = spinner.selectedItem.toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // write code to perform some action
            }

        }
        //Retrievo data, ora, durata


    }


}