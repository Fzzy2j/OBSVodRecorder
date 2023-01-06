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
import io.obswebsocket.community.client.OBSRemoteController
import java.io.File
import java.io.InputStreamReader
import java.util.*

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

fun main(args: Array<String>) {
    val transport = GoogleNetHttpTransport.newTrustedTransport()
    val credentials = getCredentials(transport)
    googleSheetService = Sheets.Builder(transport, factory, credentials).setApplicationName("FzzyApexGraphics").build()

    Thread {
        while (true) {
            oldIdentifier = identifier
            identifier = SheetsUtil.readFromSheet("1jnOGzo2GX3omiMcqxLRNACdYHYz3pkiYGiqbGFsHnXk", "Multiversus!B3")[0][0]
            Thread.sleep(5000)
        }
    }.start()

    controller = OBSRemoteController.builder()
        .host("localhost")
        .port(4455)
        .lifecycle().onReady {
            controller.startRecord(5)
        }.and()
        .build()
    controller.connect()
    Thread.sleep(1000)
    while (true) {
        Thread.sleep(100)
        if (identifier != oldIdentifier) {
            oldIdentifier = identifier
            if (!VMix.isGameplay() && !startRecordingOnNextGameplay) {
                controller.stopRecord(5)
                startRecordingOnNextGameplay = true
            }
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