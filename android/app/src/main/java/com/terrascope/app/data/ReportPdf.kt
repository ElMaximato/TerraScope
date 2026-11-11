package com.terrascope.app.data

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.terrascope.app.data.model.ZoneReport
import java.io.File

/** Genera el reporte en PDF (con PdfDocument del sistema, sin librerías) y lo comparte. */
object ReportPdf {
    private const val W = 595
    private const val H = 842
    private const val M = 44f

    private fun num(v: Double) = if (v >= 100) "%,.0f".format(v) else "%.1f".format(v)
    private fun conf(c: String?) = when (c) { "high" -> "alta"; "medium" -> "media"; "low" -> "baja"; else -> "—" }

    fun write(context: Context, r: ZoneReport): File {
        val doc = PdfDocument()
        val body = Paint().apply { textSize = 10.5f; color = 0xFF222222.toInt(); isAntiAlias = true }
        val bold = Paint(body).apply { typeface = Typeface.DEFAULT_BOLD }
        val h1 = Paint(bold).apply { textSize = 20f }
        val h2 = Paint(bold).apply { textSize = 13f; color = 0xFF0F4C5C.toInt() }
        val muted = Paint(body).apply { color = 0xFF777777.toInt(); textSize = 9f }

        var pageNo = 0
        var page: PdfDocument.Page? = null
        var y = 0f
        fun newPage() {
            page?.let { doc.finishPage(it) }
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, pageNo).create())
            y = M + 10f
        }
        fun ensure(h: Float) { if (page == null || y + h > H - M) newPage() }
        fun line(text: String, paint: Paint, indent: Float = 0f, gap: Float = 4f) {
            val maxW = W - 2 * M - indent
            val words = text.split(" ")
            var cur = ""
            val lines = mutableListOf<String>()
            for (w in words) {
                val t = if (cur.isEmpty()) w else "$cur $w"
                if (paint.measureText(t) > maxW && cur.isNotEmpty()) { lines.add(cur); cur = w } else cur = t
            }
            if (cur.isNotEmpty()) lines.add(cur)
            for (l in lines) {
                ensure(paint.textSize + gap)
                page!!.canvas.drawText(l, M + indent, y + paint.textSize, paint)
                y += paint.textSize + gap
            }
        }
        fun section(t: String) { y += 8f; ensure(30f); line(t, h2, gap = 6f) }
        fun bullets(items: List<String>) = items.forEach { line("• $it", body, indent = 8f) }

        newPage()
        line("TerraScope · Reporte de zona", muted)
        line(r.zoneName, h1, gap = 8f)
        line("Perfil: ${r.profileLabel}  ·  Generado: ${r.generatedAt.take(10)}", muted, gap = 10f)
        line(r.summary, bold, gap = 6f)

        r.aiSummary?.let {
            section("Reporte IA")
            line(it, body, indent = 8f)
            line("Redactado por IA solo con los datos del reporte; cifras verificadas contra ellos.", muted, indent = 8f)
        }

        section("Datos del Census")
        r.indicators.forEach {
            val moe = it.marginOfError?.let { m -> " (± ${num(m)})" } ?: ""
            val rank = if (it.rankPosition != null) " · ${it.rankPosition}.º de ${it.rankOf}" else ""
            line("${it.name}: ${num(it.value)} ${it.unit ?: ""}$moe$rank", body, indent = 8f)
        }
        if (r.score != null) {
            section("Puntaje")
            line("${r.score}/100 · datos ${conf(r.confidence)}" + (if (r.rankPosition != null) " · ${r.rankPosition}.º de ${r.rankOf} zonas" else ""), body, indent = 8f)
        }
        if (r.strengths.isNotEmpty()) { section("A favor de la zona"); bullets(r.strengths.map { it.text }) }
        if (r.warnings.isNotEmpty()) { section("Lo que hay que cuidar"); bullets(r.warnings.map { it.text }) }
        if (r.missingData.isNotEmpty()) { section("Datos faltantes"); bullets(r.missingData) }
        section("Antes de decidir, revisa en campo"); bullets(r.verification)
        section("Cómo leer este reporte (limitaciones)"); bullets(r.limitations)
        section("Fuentes"); bullets(r.sources.map { "${it.what}: ${it.name}${it.period?.let { p -> " ($p)" } ?: ""}" })
        section("Metodología"); line(r.methodology, body, indent = 8f)

        page?.let { doc.finishPage(it) }
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val safe = r.zoneName.replace(Regex("[^A-Za-z0-9]+"), "_").trim('_').ifEmpty { "zona" }
        val file = File(dir, "TerraScope_$safe.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    fun share(context: Context, r: ZoneReport) {
        val file = write(context, r)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Reporte TerraScope · ${r.zoneName}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Compartir reporte").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
