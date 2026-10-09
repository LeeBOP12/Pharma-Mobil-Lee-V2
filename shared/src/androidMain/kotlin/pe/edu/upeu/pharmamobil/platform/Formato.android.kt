package pe.edu.upeu.pharmamobil.platform

import java.text.NumberFormat
import java.util.Locale

 fun formatearSoles(valor: Double): String =
    NumberFormat.getCurrencyInstance(Locale("es", "PE")).format(valor)
