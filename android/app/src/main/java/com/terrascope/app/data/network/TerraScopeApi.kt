package com.terrascope.app.data.network

import com.terrascope.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object TerraScopeApi {
    // 127.0.0.1 apunta al localhost de tu PC desde el emulador de Android
    var baseUrl = "http://127.0.0.1:3000/api"

    // ── Zonas ───────────────────────────────────────────────────────────────

    suspend fun fetchZones(token: String): List<ZoneItem> = withContext(Dispatchers.IO) {
        val json = getJson("$baseUrl/zones", token)
        val arr = json.optJSONArray("zones") ?: JSONArray()
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            ZoneItem(
                id = obj.getInt("id"),
                code = obj.getString("code"),
                name = obj.getString("name"),
                level = obj.getString("level"),
                friendlyTitle = obj.optJSONObject("friendly")?.optString("title", "")?.ifEmpty { null },
                kind = obj.optJSONObject("friendly")?.optString("kind", "")?.ifEmpty { null }
            )
        }
    }

    suspend fun fetchZoneDetail(id: Int, token: String): ZoneDetail = withContext(Dispatchers.IO) {
        val json = getJson("$baseUrl/zones/$id", token)
        val z = json.getJSONObject("zone")
        val indArr = z.optJSONArray("indicators") ?: JSONArray()
        val indicators = (0 until indArr.length()).map { i ->
            val ind = indArr.getJSONObject(i)
            ZoneIndicator(
                code = ind.getString("code"),
                name = ind.getString("name"),
                value = ind.getDouble("value"),
                unit = ind.optString("unit", "").ifEmpty { null },
                marginOfError = if (ind.isNull("marginOfError")) null else ind.getDouble("marginOfError"),
                referencePeriod = ind.optString("referencePeriod", "").ifEmpty { null }
            )
        }
        // El backend devuelve boundary como objeto GeoJSON; lo serializamos a String
        val boundaryStr = if (!z.isNull("boundary")) z.getJSONObject("boundary").toString() else null
        ZoneDetail(
            id = z.getInt("id"),
            code = z.getString("code"),
            name = z.getString("name"),
            level = z.getString("level"),
            indicators = indicators,
            boundaryGeoJson = boundaryStr,
            friendly = z.optJSONObject("friendly")?.let { f ->
                val hl = f.optJSONArray("highlights") ?: JSONArray()
                ZoneFriendly(
                    title = f.optString("title", z.getString("name")),
                    kind = f.optString("kind", "").ifEmpty { null },
                    codeLabel = f.optString("codeLabel", ""),
                    description = f.optString("description", ""),
                    highlights = (0 until hl.length()).map { k ->
                        val h = hl.getJSONObject(k)
                        ZoneHighlight(
                            code = h.optString("code", ""),
                            title = h.optString("title", ""),
                            value = h.optString("value", ""),
                            help = h.optString("help", ""),
                            comparison = h.optString("comparison", "").ifEmpty { null }
                        )
                    }
                )
            }
        )
    }

    suspend fun fetchZoneScore(id: Int, token: String, profile: String = "retail"): ZoneScore = withContext(Dispatchers.IO) {
        val j = getJson("$baseUrl/zones/$id/score?profile=$profile", token).getJSONObject("score")
        val arr = j.optJSONArray("factors") ?: JSONArray()
        ZoneScore(
            status = j.getString("status"),
            score = if (j.isNull("score")) null else j.getInt("score"),
            confidence = if (j.isNull("confidence")) null else j.getString("confidence"),
            profile = j.optString("profile", ""),
            profileLabel = j.optString("profileLabel", ""),
            note = if (j.isNull("note")) null else j.optString("note"),
            comparedWith = j.optInt("comparedWith", 0),
            reason = if (j.isNull("reason")) null else j.getString("reason"),
            factors = (0 until arr.length()).map { i ->
                val f = arr.getJSONObject(i)
                ScoreFactor(
                    code = f.getString("code"),
                    label = f.getString("label"),
                    weight = f.getInt("weight"),
                    points = f.getDouble("points"),
                    normalized = f.getDouble("normalized")
                )
            }
        )
    }

    suspend fun fetchProfiles(token: String): List<InvestProfile> = withContext(Dispatchers.IO) {
        val arr = getJson("$baseUrl/zones/profiles", token).optJSONArray("profiles") ?: JSONArray()
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            InvestProfile(o.getString("id"), o.getString("label"), o.optString("description", ""))
        }
    }

    suspend fun matchProfile(text: String, token: String): ProfileMatch? = withContext(Dispatchers.IO) {
        val q = java.net.URLEncoder.encode(text, "UTF-8")
        val json = getJson("$baseUrl/zones/profiles/match?q=$q", token)
        if (json.isNull("match")) return@withContext null
        val m = json.getJSONObject("match")
        val words = m.optJSONArray("matched") ?: JSONArray()
        ProfileMatch(
            profile = m.getString("profile"),
            label = m.getString("label"),
            matched = (0 until words.length()).map { words.getString(it) }
        )
    }

    suspend fun fetchRanking(profile: String, token: String): Ranking = withContext(Dispatchers.IO) {
        val r = getJson("$baseUrl/zones/ranking?profile=$profile", token).getJSONObject("ranking")
        val arr = r.optJSONArray("items") ?: JSONArray()
        Ranking(
            profile = r.getString("profile"),
            label = r.getString("label"),
            description = r.optString("description", ""),
            note = r.optString("note", ""),
            items = (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                RankedZone(
                    zoneId = o.getInt("zoneId"),
                    code = o.getString("code"),
                    name = o.getString("name"),
                    status = o.getString("status"),
                    score = if (o.isNull("score")) null else o.getInt("score"),
                    confidence = if (o.isNull("confidence")) null else o.getString("confidence"),
                    friendlyTitle = o.optString("friendlyTitle", "").ifEmpty { null }
                )
            }
        )
    }

    // ── Autenticación ───────────────────────────────────────────────────────

    /**
     * Intenta login. Devuelve token o lanza excepción con el código de error del servidor.
     */
    suspend fun login(email: String, password: String): Pair<String, AuthUser> =
        withContext(Dispatchers.IO) {
            val body = JSONObject().apply {
                put("email", email)
                put("password", password)
            }
            val res = postJson("$baseUrl/auth/login", body)
            val code = res.getInt("__status")
            if (code !in 200..299) {
                val errCode = res.optJSONObject("error")?.optString("code") ?: "unknown"
                throw ApiException(code, errCode)
            }
            val token = res.getString("token")
            val u = res.getJSONObject("user")
            Pair(
                token,
                AuthUser(
                    id = u.getInt("id"),
                    email = u.getString("email"),
                    displayName = u.optString("displayName", "").ifEmpty { null }
                )
            )
        }

    /**
     * Registra un nuevo usuario. Devuelve token o lanza excepción.
     */
    suspend fun register(
        email: String,
        password: String,
        displayName: String
    ): Pair<String, AuthUser> = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("email", email)
            put("password", password)
            put("displayName", displayName)
        }
        val res = postJson("$baseUrl/auth/register", body)
        val code = res.getInt("__status")
        if (code !in 200..299) {
            val errCode = res.optJSONObject("error")?.optString("code") ?: "unknown"
            throw ApiException(code, errCode)
        }
        val token = res.getString("token")
        val u = res.getJSONObject("user")
        Pair(
            token,
            AuthUser(
                id = u.getInt("id"),
                email = u.getString("email"),
                displayName = u.optString("displayName", "").ifEmpty { null }
            )
        )
    }

    suspend fun fetchMe(token: String): AuthUser = withContext(Dispatchers.IO) {
        val json = getJson("$baseUrl/auth/me", token)
        val u = json.getJSONObject("user")
        AuthUser(
            id = u.getInt("id"),
            email = u.getString("email"),
            displayName = u.optString("displayName", "").ifEmpty { null }
        )
    }

    // ── Propiedades ─────────────────────────────────────────────────────────

    private fun parseProperty(obj: JSONObject) = PropertyItem(
        id = obj.getInt("id"),
        name = obj.getString("name"),
        propertyType = obj.optString("propertyType", "other"),
        address = if (obj.isNull("address")) null else obj.optString("address").ifEmpty { null },
        lat = obj.getDouble("lat"),
        lon = obj.getDouble("lon"),
        areaValue = if (obj.isNull("areaValue")) null else obj.getDouble("areaValue"),
        areaUnit = if (obj.isNull("areaUnit")) null else obj.optString("areaUnit").ifEmpty { null },
        askingPrice = if (obj.isNull("askingPrice")) null else obj.getDouble("askingPrice"),
        notes = if (obj.isNull("notes")) null else obj.optString("notes").ifEmpty { null },
        zoneId = if (obj.isNull("zoneId")) null else obj.getInt("zoneId"),
        createdAt = obj.optString("createdAt", "")
    )

    suspend fun fetchProperties(token: String): List<PropertyItem> = withContext(Dispatchers.IO) {
        val json = getJson("$baseUrl/properties", token)
        val arr = json.optJSONArray("properties") ?: JSONArray()
        (0 until arr.length()).map { i -> parseProperty(arr.getJSONObject(i)) }
    }

    private fun draftBody(draft: PropertyDraft): JSONObject {
        val body = JSONObject()
            .put("name", draft.name)
            .put("propertyType", draft.propertyType)
            .put("lat", draft.lat)
            .put("lon", draft.lon)
        draft.askingPrice?.let { body.put("askingPrice", it) }
        if (draft.areaValue != null && draft.areaUnit != null) {
            body.put("areaValue", draft.areaValue).put("areaUnit", draft.areaUnit)
        }
        draft.notes?.let { body.put("notes", it) }
        return body
    }

    private fun propertyFromResponse(res: JSONObject): PropertyItem {
        val code = res.optInt("__status", 0)
        if (code !in 200..299) {
            val msg = res.optJSONObject("error")?.optString("message") ?: "No se pudo guardar la propiedad"
            throw ApiException(code, "property_error", msg)
        }
        return parseProperty(res.getJSONObject("property"))
    }

    suspend fun createProperty(draft: PropertyDraft, token: String): PropertyItem = withContext(Dispatchers.IO) {
        propertyFromResponse(postJsonWithAuth("$baseUrl/properties", draftBody(draft), token, "POST"))
    }

    /** Reemplaza todos los datos de la propiedad (campos opcionales vacíos se borran). */
    suspend fun updateProperty(id: Int, draft: PropertyDraft, token: String): PropertyItem = withContext(Dispatchers.IO) {
        propertyFromResponse(postJsonWithAuth("$baseUrl/properties/$id", draftBody(draft), token, "PUT"))
    }

    suspend fun deleteProperty(id: Int, token: String) = withContext(Dispatchers.IO) {
        val res = deleteJson("$baseUrl/properties/$id", token)
        val code = res.optInt("__status", 0)
        if (code !in 200..299) throw ApiException(code, "property_error")
    }

    // ── Watchlist ────────────────────────────────────────────────────────────

    suspend fun fetchWatchlist(token: String): List<ZoneItem> = withContext(Dispatchers.IO) {
        val json = getJson("$baseUrl/watchlist", token)
        // El backend responde { items: [ { id, zoneId, note, zone: { code, name, level } } ] }
        val arr = json.optJSONArray("items") ?: JSONArray()
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            val zone = obj.getJSONObject("zone")
            ZoneItem(
                id = obj.getInt("zoneId"),
                code = zone.getString("code"),
                name = zone.getString("name"),
                level = zone.getString("level")
            )
        }
    }

    suspend fun addToWatchlist(zoneId: Int, token: String) = withContext(Dispatchers.IO) {
        // El backend espera POST /watchlist con { "zoneId": N } (201 si se creó, 200 si ya existía).
        val res = postJsonWithAuth("$baseUrl/watchlist", JSONObject().put("zoneId", zoneId), token, "POST")
        val code = res.getInt("__status")
        if (code !in 200..299) throw ApiException(code, "watchlist_error")
    }

    suspend fun removeFromWatchlist(zoneId: Int, token: String) = withContext(Dispatchers.IO) {
        val res = deleteJson("$baseUrl/watchlist/$zoneId", token)
        val code = res.getInt("__status")
        if (code !in 200..299) throw ApiException(code, "watchlist_error")
    }

    // ── Sesiones de campo ────────────────────────────────────────────────────

    suspend fun fetchFieldSessions(token: String): List<FieldSession> = withContext(Dispatchers.IO) {
        val json = getJson("$baseUrl/field/sessions", token)
        val arr = json.optJSONArray("sessions") ?: JSONArray()
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            // observaciones anidadas o conteo
            val obsArr = obj.optJSONArray("observations")
            val obsCount = obsArr?.length() ?: obj.optInt("observationCount", 0)
            FieldSession(
                id = obj.getString("id"),
                deviceId = obj.getString("deviceId"),
                startedAt = obj.getString("startedAt"),
                endedAt = obj.optString("endedAt", "").ifEmpty { null },
                observationCount = obsCount
            )
        }
    }

    /** Sube puntos del reloj en lotes de 100. Devuelve los ids que el servidor ya tiene (creado, actualizado o repetido). */
    suspend fun syncObservations(points: List<JSONObject>, token: String): Set<String> = withContext(Dispatchers.IO) {
        val confirmed = mutableSetOf<String>()
        points.chunked(100).forEach { chunk ->
            val arr = JSONArray()
            chunk.forEach { arr.put(it) }
            val res = postJsonWithAuth("$baseUrl/field/sync", JSONObject().put("observations", arr), token, "POST")
            val code = res.optInt("__status", 0)
            if (code !in 200..299) throw ApiException(code, "sync_error")
            val results = res.optJSONObject("results")?.optJSONArray("observations") ?: JSONArray()
            for (i in 0 until results.length()) {
                val r = results.getJSONObject(i)
                if (r.optString("status") in setOf("created", "updated", "duplicate")) confirmed.add(r.getString("id"))
            }
        }
        confirmed
    }

    suspend fun fetchObservations(token: String): List<FieldPoint> = withContext(Dispatchers.IO) {
        val json = getJson("$baseUrl/field/observations?limit=200", token)
        val arr = json.optJSONArray("observations") ?: JSONArray()
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            FieldPoint(
                id = o.getString("id"),
                tag = o.optString("tag", "needs_research"),
                lat = o.getDouble("lat"),
                lon = o.getDouble("lon"),
                observedAt = o.optString("observedAt", ""),
                note = if (o.isNull("note")) null else o.optString("note").ifBlank { null }
            )
        }
    }

    suspend fun fetchAnalysis(token: String, profile: String, withAi: Boolean = false): LocationAnalysis = withContext(Dispatchers.IO) {
        val a = getJson("$baseUrl/field/analysis?profile=$profile" + if (withAi) "&ai=1" else "", token, if (withAi) AI_TIMEOUT_MS else 8000).getJSONObject("analysis")
        val arr = a.optJSONArray("items") ?: JSONArray()
        val notes = a.optJSONArray("notes") ?: JSONArray()
        val ai = a.optJSONObject("ai")
        LocationAnalysis(
            profileLabel = a.getJSONObject("profile").getString("label"),
            summary = a.getString("summary"),
            items = (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                AnalysisItem(
                    name = o.optString("name"), kind = o.optString("kind"),
                    zoneName = if (o.isNull("zoneName")) null else o.getString("zoneName"),
                    score = if (o.isNull("score")) null else o.getInt("score"),
                    confidence = if (o.isNull("confidence")) null else o.getString("confidence"),
                    status = o.optString("status")
                )
            },
            notes = (0 until notes.length()).map { notes.getString(it) },
            aiText = ai?.takeIf { it.optString("status") == "ok" && !it.isNull("text") }?.getString("text"),
            aiStatus = ai?.optString("status") ?: "disabled"
        )
    }

    suspend fun fetchZoneReport(id: Int, token: String, profile: String, withAi: Boolean = false): ZoneReport = withContext(Dispatchers.IO) {
        val r = getJson("$baseUrl/zones/$id/report?profile=$profile" + if (withAi) "&ai=1" else "", token, if (withAi) AI_TIMEOUT_MS else 8000).getJSONObject("report")
        fun strings(a: JSONArray?) = (0 until (a?.length() ?: 0)).map { a!!.getString(it) }
        fun items(a: JSONArray?) = (0 until (a?.length() ?: 0)).map {
            val o = a!!.getJSONObject(it)
            ReportItem(if (o.isNull("ref")) null else o.getString("ref"), o.getString("text"))
        }
        val inds = r.optJSONArray("indicators") ?: JSONArray()
        val srcs = r.optJSONArray("sources") ?: JSONArray()
        val sc = r.optJSONObject("score")
        ZoneReport(
            zoneName = r.getJSONObject("zone").getString("name"),
            profileLabel = r.getJSONObject("profile").getString("label"),
            generatedAt = r.optString("generatedAt"),
            summary = r.getString("summary"),
            score = sc?.getInt("value"),
            confidence = if (sc == null || sc.isNull("confidence")) null else sc.getString("confidence"),
            indicators = (0 until inds.length()).map {
                val o = inds.getJSONObject(it)
                val rank = o.optJSONObject("rank")
                ReportIndicator(
                    name = o.optString("name"), value = o.getDouble("value"),
                    unit = if (o.isNull("unit")) null else o.optString("unit"),
                    marginOfError = if (o.isNull("marginOfError")) null else o.getDouble("marginOfError"),
                    variationPct = if (o.isNull("variationPct")) null else o.getDouble("variationPct"),
                    rankPosition = rank?.getInt("position"), rankOf = rank?.getInt("of"),
                    period = if (o.isNull("referencePeriod")) null else o.optString("referencePeriod")
                )
            },
            strengths = items(r.optJSONArray("strengths")),
            warnings = items(r.optJSONArray("warnings")),
            missingData = strings(r.optJSONArray("missingData")),
            verification = strings(r.optJSONArray("verification")),
            limitations = strings(r.optJSONArray("limitations")),
            sources = (0 until srcs.length()).map {
                val o = srcs.getJSONObject(it)
                ReportSource(
                    o.optString("what"),
                    listOf(o.optString("organization"), o.optString("name")).filter { x -> x.isNotBlank() }.joinToString(" "),
                    if (o.isNull("period")) null else o.optString("period")
                )
            },
            methodology = r.optString("methodology"),
            aiStatus = r.optJSONObject("ai")?.optString("status") ?: "none",
            rankPosition = sc?.optJSONObject("rank")?.getInt("position"),
            rankOf = sc?.optJSONObject("rank")?.getInt("of"),
            factors = (0 until (sc?.optJSONArray("factors")?.length() ?: 0)).map {
                val f = sc!!.getJSONArray("factors").getJSONObject(it)
                ReportFactor(f.optString("label"), f.optInt("weight"), f.optDouble("points"), f.optDouble("normalized"))
            },
            aiSummary = r.optJSONObject("ai")?.takeIf { it.optString("status") == "ok" && !it.isNull("text") }?.getString("text")
        )
    }

    /** Edita nombre (note) y etiqueta de un punto. La ubicación no se edita. */
    suspend fun updateObservation(id: String, note: String, tag: String, token: String) = withContext(Dispatchers.IO) {
        val body = JSONObject().put("note", note.trim()).put("tag", tag)
        val res = postJsonWithAuth("$baseUrl/field/observations/$id", body, token, "PUT")
        val code = res.optInt("__status", 0)
        if (code !in 200..299) throw ApiException(code, "observation_error")
    }

    suspend fun deleteObservation(id: String, token: String) = withContext(Dispatchers.IO) {
        val res = deleteJson("$baseUrl/field/observations/$id", token)
        val code = res.optInt("__status", 0)
        if (code !in 200..299) throw ApiException(code, "observation_error")
    }

    // ── HTTP helpers ─────────────────────────────────────────────────────────

    /** La IA puede tardar (el backend reintenta si Google está saturado). */
    private const val AI_TIMEOUT_MS = 45000

    private fun getJson(urlStr: String, token: String? = null, readTimeoutMs: Int = 8000): JSONObject {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = readTimeoutMs
            setRequestProperty("Accept", "application/json")
            token?.let { setRequestProperty("Authorization", "Bearer $it") }
        }
        return readResponse(conn)
    }

    /**
     * POST con body JSON opcional. Devuelve JSONObject con campo "__status" inyectado.
     */
    private fun postJson(urlStr: String, body: JSONObject?): JSONObject {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 8000
            readTimeout = 8000
            doOutput = body != null
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }
        body?.let { OutputStreamWriter(conn.outputStream).use { w -> w.write(it.toString()) } }
        return readResponseRaw(conn)
    }

    private fun postJsonWithAuth(
        urlStr: String,
        body: JSONObject?,
        token: String,
        method: String = "POST"
    ): JSONObject {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 8000
            readTimeout = 8000
            doOutput = body != null
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
        }
        body?.let { OutputStreamWriter(conn.outputStream).use { w -> w.write(it.toString()) } }
        return readResponseRaw(conn)
    }

    private fun deleteJson(urlStr: String, token: String): JSONObject {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "DELETE"
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
        }
        return readResponseRaw(conn)
    }

    /** Lee la respuesta y lanza excepción si status no es 2xx. */
    private fun readResponse(conn: HttpURLConnection): JSONObject {
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream.bufferedReader().use { it.readText() }
        conn.disconnect()
        if (code !in 200..299) throw ApiException(code, "http_error", text)
        return JSONObject(text)
    }

    /** Lee la respuesta y agrega "__status" sin lanzar excepción. */
    private fun readResponseRaw(conn: HttpURLConnection): JSONObject {
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
        conn.disconnect()
        return try {
            JSONObject(text).apply { put("__status", code) }
        } catch (e: Exception) {
            JSONObject().apply { put("__status", code) }
        }
    }
}

/** Excepción tipada para errores de la API. */
class ApiException(
    val statusCode: Int,
    val errorCode: String,
    val rawBody: String? = null
) : Exception("HTTP $statusCode [$errorCode]")
