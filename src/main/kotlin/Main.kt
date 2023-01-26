import com.github.sarxos.webcam.Webcam
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
import com.google.gson.Gson
import io.obswebsocket.community.client.OBSRemoteController
import org.im4java.process.ProcessStarter
import thumbnails.GuiltyGearGenerator
import thumbnails.ThumbnailGenerator
import java.io.*
import java.util.*
import java.util.concurrent.Executors


lateinit var googleSheetService: Sheets
val factory: GsonFactory = GsonFactory.getDefaultInstance()
lateinit var controller: OBSRemoteController
var paused = false
val gson = Gson()


val identifiers = arrayOf(
    VodIdentifier(
        hashMapOf("P1Name" to "Multiversus!B3", "P2Name" to "Multiversus!E3", "Round" to "Multiversus!H2"),
        hashMapOf("BestOf" to "Multiversus!H3")
    ),
    VodIdentifier(
        hashMapOf("P1Name" to "Guilty Gear!B3", "P2Name" to "Guilty Gear!E3", "Round" to "Guilty Gear!H2"),
        hashMapOf("BestOf" to "Guilty Gear!H3"),
        GuiltyGearGenerator
    ),
    VodIdentifier(hashMapOf("Game" to "Apex!A15"), hashMapOf("ChampionSquad" to "Apex!A6"))
)


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

var startRecordingOnNextGameplay = true

fun updateIdentifier() {
    val cellList = arrayListOf<String>()
    for (id in identifiers) {
        cellList.addAll(id.mainIdentifiers.values)
        cellList.addAll(id.tags.values)
    }
    try {
        val sheet = SheetsUtil.batchRead("1jnOGzo2GX3omiMcqxLRNACdYHYz3pkiYGiqbGFsHnXk", *cellList.toTypedArray())
        for (id in identifiers) {
            id.update(sheet)
        }
    } catch (e: Exception) {
        println("Failed to update identifers: ${e.message}")
    }
    for (id in identifiers) {
        threads.submit { id.thumbnailGenerator?.onIdenfifierUpdate() }
    }
}

val threads = Executors.newCachedThreadPool()

fun main(args: Array<String>) {
    ProcessStarter.setGlobalSearchPath(File("").absolutePath)
    System.load(File("opencv_java470.dll").absolutePath)

    val transport = GoogleNetHttpTransport.newTrustedTransport()
    val credentials = getCredentials(transport)
    googleSheetService = Sheets.Builder(transport, factory, credentials).setApplicationName("FzzyApexGraphics").build()
    updateIdentifier()

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

        val mainIds = arrayListOf<String>()
        val tags = arrayListOf<String>()
        for (id in identifiers) {
            if (id.anyChanges()) {
                mainIds.add(id.getOldIdentifier())
                tags.addAll(id.getTagValues())
                id.consumeChanges()
            }
        }
        if (mainIds.isNotEmpty()) {
            println("identifier changed: ${mainIds.joinToString(" ")}")
            if (!VMix.isGameplay() && !startRecordingOnNextGameplay) {
                controller.stopRecord(5)
                startRecordingOnNextGameplay = true
                val newName = "${mainIds.joinToString(" ")}.mp4"
                controller.getOutputSettings("adv_file_output") {
                    val file = File(it.outputSettings.get("path").asString)
                    //val directory = File(file.parent, tags.joinToString("\\"))
                    val rename = File(file.parent, newName.replace("|", ""))
                    println("vod file: ${rename.absolutePath}\ntags: ${tags.joinToString()}")
                    Thread {
                        Thread.sleep(4000)
                        file.renameTo(rename)
                    }.start()
                }
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