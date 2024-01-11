package com.unibs.consbs.profile

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.unibs.consbs.Materia_LivelloStudio
import com.unibs.consbs.R
import com.unibs.consbs.Studente_Materia
import com.unibs.consbs.data.Materia

class ModificaMaterieAdapter(val context: Context, private val materieList: ArrayList<Materia>): RecyclerView.Adapter<ModificaMaterieAdapter.ModificaMaterieViewHolder>()  {

    lateinit var mAuth: FirebaseAuth

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModificaMaterieViewHolder {
        mAuth = Firebase.auth
        val view = LayoutInflater.from(context).inflate(R.layout.layout_materia, parent, false)
        return ModificaMaterieAdapter.ModificaMaterieViewHolder(view)
    }

    override fun getItemCount(): Int {
        return materieList.size
    }

    override fun onBindViewHolder(holder: ModificaMaterieViewHolder, position: Int) {
        val currentMateria = materieList[position]

        holder.txt_nome_materia.text = currentMateria.nome.toString()

        holder.check_selezionato.isChecked = false
        val mDbRef = Firebase.database.reference
        val materieRef = mDbRef.child("studenti_materie").orderByChild("id_studente").equalTo(mAuth.currentUser?.uid.toString())

        materieRef.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for ( materiaSnapshot in snapshot.children ) {
                    var studente_materia = materiaSnapshot.getValue(Studente_Materia::class.java)

                    if ( studente_materia?.id_materia == currentMateria.id ) {
                        holder.check_selezionato.isChecked = true
                        currentMateria.isSelected = true
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("GET studenti_materie", "Errore: ${error.message}")
            }

        })

        holder.check_selezionato.setOnCheckedChangeListener{ _, isChecked ->
            currentMateria.isSelected = isChecked

        }
    }

    class ModificaMaterieViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var txt_nome_materia = itemView.findViewById<TextView>(R.id.nome_materia)
        val check_selezionato = itemView.findViewById<CheckBox>(R.id.materia_selezionata)
    }

}