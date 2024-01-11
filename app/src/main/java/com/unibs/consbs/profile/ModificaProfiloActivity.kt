package com.unibs.consbs.profile

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
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
import com.unibs.consbs.R
import com.unibs.consbs.data.Studente
import com.unibs.consbs.databinding.ModificaProfiloBinding
import com.unibs.consbs.utils.FirebaseUtils
import com.unibs.consbs.utils.Utils

class ModificaProfiloActivity: BaseActivity() {

    lateinit var binding: ModificaProfiloBinding
    private lateinit var db: DBHelper
    private lateinit var mDbRef: DatabaseReference


    override fun onCreate(savedInstanceState: Bundle?) {

        mAuth = Firebase.auth
        val currentUser = mAuth.currentUser

        super.onCreate(savedInstanceState)
        binding = ModificaProfiloBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Utils.setCurrentActivty("ModificaProfiloActivity")

        FirebaseUtils.checkUserLoggedIn(this)

        binding.menu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        // Menù
        setNavigationViewSelectedItem("Profilo")
        setOptionsItemsListener()

        mDbRef = Firebase.database.reference

        binding.inserisciCellulare.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.inserisciCellulare.setBackgroundResource(R.color.white)
                binding.inserisciCellulare.setTextColor(getResources().getColor(R.color.black))

                binding.confermaCellulare.setBackgroundResource(R.color.white)
                binding.confermaCellulare.setTextColor(getResources().getColor(R.color.black))

                binding.messaggioErrore.text = ""
            }

            override fun afterTextChanged(s: Editable?) {

            }

        })

        binding.confermaCellulare.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.inserisciCellulare.setBackgroundResource(R.color.white)
                binding.inserisciCellulare.setTextColor(getResources().getColor(R.color.black))

                binding.confermaCellulare.setBackgroundResource(R.color.white)
                binding.confermaCellulare.setTextColor(getResources().getColor(R.color.black))

                binding.messaggioErrore.text = ""
            }

            override fun afterTextChanged(s: Editable?) {

            }

        })

        binding.salvaModifica.setOnClickListener{

            val cellulare_inserito = binding.inserisciCellulare.text.trim()
            val cellulare_confermato = binding.confermaCellulare.text.trim()

            if ( cellulare_inserito.isEmpty() || !Utils.isPhoneNumberValid(cellulare_inserito.toString()) ) {
                binding.inserisciCellulare.setBackgroundResource(R.drawable.edit_text_modifica_profilo)
                binding.inserisciCellulare.setTextColor(getResources().getColor(R.color.red_palette))

                binding.messaggioErrore.text = "Il numero di cellulare inserito non è valido!"
            } else if ( cellulare_confermato.isEmpty() || !Utils.isPhoneNumberValid(cellulare_confermato.toString()) ) {
                binding.confermaCellulare.setBackgroundResource(R.drawable.edit_text_modifica_profilo)
                binding.confermaCellulare.setTextColor(getResources().getColor(R.color.red_palette))

                binding.messaggioErrore.text = "Il numero di cellulare inserito di conferma non è valido!"
            } else if ( cellulare_inserito.toString() != cellulare_confermato.toString() ) {
                binding.inserisciCellulare.setBackgroundResource(R.drawable.edit_text_modifica_profilo)
                binding.inserisciCellulare.setTextColor(getResources().getColor(R.color.red_palette))

                binding.confermaCellulare.setBackgroundResource(R.drawable.edit_text_modifica_profilo)
                binding.confermaCellulare.setTextColor(getResources().getColor(R.color.red_palette))

                binding.messaggioErrore.text = "I due numeri non corrispondono!"
            } else {
                mDbRef.child("studenti").child(currentUser?.uid.toString()).addListenerForSingleValueEvent(object: ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if ( snapshot.exists() ) {
                            val studente = snapshot.getValue(Studente::class.java)
                            val studente_cellulare = studente?.cellulare.toString()

                            if ( studente_cellulare != binding.inserisciCellulare.text.toString() ) {
                                db = DBHelper()
                                db.aggiorna_cellulare_user(currentUser?.uid.toString(), binding.inserisciCellulare.text.toString())

                                Toast.makeText(this@ModificaProfiloActivity, "Aggiornamento effettuato!", Toast.LENGTH_LONG).show()

                                val intent = Intent(this@ModificaProfiloActivity, ProfiloActivity::class.java)
                                startActivity(intent)
                                finish()
                            } else {
                                binding.messaggioErrore.text = "Il numero inserito è uguale a quello già registrato!"
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.d("GET profilo cell", "Errore: ${error.message}")
                    }

                })
            }

        }

        binding.modificaMaterie.setOnClickListener{

            val intent = Intent(this@ModificaProfiloActivity, ModificaMaterieActivity::class.java)
            startActivity(intent)
            finish()

        }

    }

}