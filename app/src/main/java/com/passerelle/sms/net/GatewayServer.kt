package com.passerelle.sms.net

import android.content.Context
import com.passerelle.sms.data.SmsJob
import com.passerelle.sms.data.SmsRepository
import com.passerelle.sms.data.SmsStatus
import com.passerelle.sms.sms.PhoneNormalizer
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

class GatewayServer(
    private val context: Context,
    port: Int,
    private val apiKey: String,
    private val repository: SmsRepository
) : NanoHTTPD("0.0.0.0", port) {

    override fun serve(session: IHTTPSession): Response {
        val remote = session.remoteIpAddress
        if (!WifiInfo.isPrivateIpv4(remote)) {
            return json(Response.Status.FORBIDDEN, errorBody("Réseau non autorisé. Wi-Fi local uniquement."))
        }

        val cors = cors(session)
        if (session.method == Method.OPTIONS) {
            return cors
        }

        return try {
            when {
                session.method == Method.GET && session.uri == "/" -> htmlDashboard()
                session.method == Method.GET && session.uri == "/health" -> getHealth(session)
                session.method == Method.GET && session.uri == "/sms" -> getSmsList(session)
                session.method == Method.GET && session.uri.startsWith("/sms/") -> getSmsOne(session)
                session.method == Method.POST && (session.uri == "/sms" || session.uri == "/api/sms") -> postSms(session)
                else -> json(Response.Status.NOT_FOUND, errorBody("Route inconnue"))
            }.withCors()
        } catch (t: Throwable) {
            json(
                Response.Status.INTERNAL_ERROR,
                errorBody(t.message ?: "Erreur interne")
            ).withCors()
        }
    }

    private fun getHealth(session: IHTTPSession): Response {
        unauthorized(session)?.let { return it }
        val counts = runBlocking { repository.counts() }
        val queue = JSONObject()
        counts.forEach { (key, value) -> queue.put(key, value) }
        val ips = JSONArray()
        WifiInfo.ipv4Addresses().forEach { ips.put(it) }
        val body = JSONObject()
            .put("ok", true)
            .put("ip", WifiInfo.primaryIpv4())
            .put("ips", ips)
            .put("queue", queue)
        return json(Response.Status.OK, body.toString())
    }

    private fun getSmsList(session: IHTTPSession): Response {
        unauthorized(session)?.let { return it }
        val filter = SmsStatus.fromApi(session.parameters["status"]?.firstOrNull())
        val jobs = runBlocking { repository.listAll() }
            .filter { filter == null || it.statusEnum == filter }
        val array = JSONArray()
        jobs.forEach { array.put(it.toJson()) }
        return json(Response.Status.OK, JSONObject().put("items", array).toString())
    }

    private fun getSmsOne(session: IHTTPSession): Response {
        unauthorized(session)?.let { return it }
        val id = session.uri.removePrefix("/sms/").toLongOrNull()
            ?: return json(Response.Status.BAD_REQUEST, errorBody("Identifiant invalide"))
        val job = runBlocking { repository.getById(id) }
            ?: return json(Response.Status.NOT_FOUND, errorBody("SMS introuvable"))
        return json(Response.Status.OK, job.toJson().toString())
    }

    private fun postSms(session: IHTTPSession): Response {
        unauthorized(session)?.let { return it }
        val payload = readJsonBody(session)
        val telRaw = payload.optString("tel")
            .ifBlank { payload.optString("phone") }
            .ifBlank { payload.optString("to") }
            .ifBlank { payload.optString("numero") }
        val message = payload.optString("message").ifBlank { payload.optString("text") }

        val tel = PhoneNormalizer.normalize(telRaw)
            ?: return json(
                Response.Status.BAD_REQUEST,
                errorBody("Numéro invalide. Utilisez un numéro au format +336… ou 06…")
            )
        if (message.isBlank()) {
            return json(Response.Status.BAD_REQUEST, errorBody("Le message est vide"))
        }
        if (message.length > 1000) {
            return json(Response.Status.BAD_REQUEST, errorBody("Message trop long (1000 caractères max)"))
        }

        val job = runBlocking { repository.enqueue(tel, message) }
        return json(Response.Status.ACCEPTED, job.toJson().toString())
    }

    private fun unauthorized(session: IHTTPSession): Response? {
        val provided = session.headers["x-api-key"]
            ?: session.headers["authorization"]?.removePrefix("Bearer ")?.trim()
            ?: session.parameters["token"]?.firstOrNull()
        if (provided.isNullOrBlank() || provided != apiKey) {
            return json(Response.Status.UNAUTHORIZED, errorBody("Jeton API manquant ou invalide"))
        }
        return null
    }

    private fun readJsonBody(session: IHTTPSession): JSONObject {
        val files = HashMap<String, String>()
        session.parseBody(files)
        val raw = files["postData"] ?: files["content"] ?: ""
        if (raw.isBlank()) {
            val tel = session.parameters["tel"]?.firstOrNull().orEmpty()
            val message = session.parameters["message"]?.firstOrNull().orEmpty()
            return JSONObject().put("tel", tel).put("message", message)
        }
        return JSONObject(raw)
    }

    private fun htmlDashboard(): Response {
        val html = context.assets.open("dashboard.html").bufferedReader(Charsets.UTF_8).use { it.readText() }
        return newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", html)
    }

    private fun json(status: Response.Status, body: String): Response {
        return newFixedLengthResponse(status, "application/json; charset=utf-8", body)
    }

    private fun errorBody(message: String): String =
        JSONObject().put("error", message).toString()

    private fun cors(session: IHTTPSession): Response {
        val response = newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "")
        response.addHeader("Access-Control-Allow-Origin", "*")
        response.addHeader("Access-Control-Allow-Headers", "Content-Type, X-Api-Key, Authorization")
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        val reqHeaders = session.headers["access-control-request-headers"]
        if (!reqHeaders.isNullOrBlank()) {
            response.addHeader("Access-Control-Allow-Headers", reqHeaders)
        }
        return response
    }

    private fun Response.withCors(): Response {
        addHeader("Access-Control-Allow-Origin", "*")
        addHeader("Access-Control-Allow-Headers", "Content-Type, X-Api-Key, Authorization")
        addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        return this
    }

    private fun SmsJob.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("tel", tel)
        .put("message", message)
        .put("status", status)
        .put("status_label", statusEnum.labelFr)
        .put("error", error ?: JSONObject.NULL)
        .put("attempt", attempt)
        .put("next_attempt_at", nextAttemptAt)
        .put("created_at", createdAt)
        .put("updated_at", updatedAt)
}
