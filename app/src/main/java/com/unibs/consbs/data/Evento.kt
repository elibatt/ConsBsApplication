package com.unibs.consbs.data

data class Evento(var id: String? = null,val nome: String? = null, val id_user_inserimento: String? = null, val id_aula: Int? = null,
                  val data_ora_inizio: Double = 0.0,val data_ora_fine: Double = 0.0, val prenotazione: Boolean?=null,
                  val id_studente_invitato: String?=null, val id_materia: String?=null, val ricorrente: Boolean?=false)
