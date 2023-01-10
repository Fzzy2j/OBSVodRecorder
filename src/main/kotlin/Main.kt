import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.ValueRange
import io.obswebsocket.community.client.OBSRemoteController
import java.io.File
import java.io.InputStreamReader
import java.util.*
import kotlin.collections.ArrayList

lateinit var googleSheetService: Sheets
val factory: GsonFactory = GsonFactory.getDefaultInstance()
lateinit var controller: OBSRemoteController
var paused = false



private fun getCredentials(HTTP_TRANSPORT: NetHttpTransport): Credential? {
    val scopes = Collections.singletonList(SheetsScopes.SPREADSHEETS)
    // Load client secrets.
    val `in` = File("credentials\\googleCredentials.json").inputStream()
    val clientSecrets = GoogleClientSecrets.load(factory, InputStreamReader(`in`))

    // Build flow and trigger user authorization request.
    val flow = GoogleAuthorizationCodeFlow.Builder(
        HTTP_TRANSPORT, factory, clientSecrets, scopes
    )
        .setDataStoreFactory(FileDataStoreFactory(File("")))
        .setAccessType("offline")
        .build()
    val receiver = LocalServerReceiver.Builder().setPort(8888).build()
    return AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
}

var identifier = ""
var oldIdentifier = ""
var startRecordingOnNextGameplay = true

fun updateIdentifier() {
    oldIdentifier = identifier
    val identifiers = arrayOf(
        //"Multiversus!B3",
        //"Multiversus!E3",
        //"Guilty Gear!B3",
        //"Guilty Gear!E3",
        "Apex!A15"
    )
    val sheet = SheetsUtil.batchRead("1jnOGzo2GX3omiMcqxLRNACdYHYz3pkiYGiqbGFsHnXk", *identifiers)

    val identifierValues = arrayListOf<String>()

    for (v in sheet.values) {
        if (v !is ArrayList<*>) continue
        for (l in v){
            val range = l as ValueRange
            for (va in range.values) {
                if (va !is ArrayList<*>) continue
                val values = va as List<List<String>>

                values.forEach { it.forEach { it -> identifierValues.add(it) } }
            }
        }
    }
    identifier = identifierValues.joinToString(" ")
}

fun main(args: Array<String>) {
    val transport = GoogleNetHttpTransport.newTrustedTransport()
    val credentials = getCredentials(transport)
    googleSheetService = Sheets.Builder(transport, factory, credentials).setApplicationName("FzzyApexGraphics").build()
    updateIdentifier()
    oldIdentifier = identifier

    Thread {
        while (true) {
            Thread.sleep(5000)
            updateIdentifier()
        }
    }.start()

    controller = OBSRemoteController.builder()
        .host("localhost")
        .port(4455)
        .build()
    controller.connect()
    Thread.sleep(1000)
    while (true) {
        Thread.sleep(100)
        if (identifier != oldIdentifier) {
            println("identifier changed: $oldIdentifier -> $identifier")
            if (!VMix.isGameplay() && !startRecordingOnNextGameplay) {
                controller.stopRecord(5)
                startRecordingOnNextGameplay = true
                val newName = "$oldIdentifier.mp4"
                controller.getOutputSettings("adv_file_output") {
                    val file = File(it.outputSettings.get("path").asString)
                    val rename = File(file.parent, newName)
                    println(rename.absolutePath)
                    Thread {
                        Thread.sleep(4000)
                        file.renameTo(rename)
                    }.start()
                }
            }
            oldIdentifier = identifier
        }
        if (VMix.isGameplay()) {
            if (startRecordingOnNextGameplay) {
                controller.startRecord(5)
                startRecordingOnNextGameplay = false
                paused = false
            }
            if (paused) {
                paused = false
                controller.resumeRecord(5)
            }
        }
        if (!VMix.isGameplay()) {
            if (!paused) {
                paused = true
                controller.pauseRecord(5)
            }
        }
    }
}