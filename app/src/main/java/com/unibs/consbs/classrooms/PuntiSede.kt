package com.unibs.consbs.classrooms

import com.unibs.consbs.data.Segnaposto

object PuntiSede {
    fun getPoints(): ArrayList<Segnaposto> {
        val pointsList = ArrayList<Segnaposto>()

        // Brescia 1: Corso Magenta 44
        var sede_bs1 = Segnaposto(1, "Sede Brescia 1", 45.535850, 10.225590)
        pointsList.add(sede_bs1)

        // Brescia 2: Via delle Grazie 7
        var sede_bs2 = Segnaposto(2, "Sede Brescia 2", 45.544901, 10.214716)
        pointsList.add(sede_bs2)

        // Darfo Boario Terme: Via Razziche 5
        var sede_dbt = Segnaposto(3, "Sede Darfo Boario Terme", 45.879300, 10.186760)
        pointsList.add(sede_dbt)

        return pointsList
    }
}