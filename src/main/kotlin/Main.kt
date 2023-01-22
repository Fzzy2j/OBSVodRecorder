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
import org.im4java.core.IMOperation
import org.im4java.core.ImageMagickCmd
import org.im4java.process.ProcessStarter
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.awt.Font
import java.awt.Point
import java.awt.image.BufferedImage
import java.io.*
import java.util.*
import javax.imageio.ImageIO


lateinit var googleSheetService: Sheets
val factory: GsonFactory = GsonFactory.getDefaultInstance()
lateinit var controller: OBSRemoteController
var paused = false
val gson = Gson()

val identifiers = arrayOf(
    VodIdentifier(arrayListOf("Multiversus!B3", "Multiversus!E3", "Multiversus!H2"), arrayListOf("Multiversus!H3")),
    VodIdentifier(arrayListOf("Guilty Gear!B3", "Guilty Gear!E3", "Guilty Gear!H2"), arrayListOf("Guilty Gear!H3")),
    VodIdentifier(arrayListOf("Apex!A15"), arrayListOf("Apex!A6"))
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
        cellList.addAll(id.mainIdentifiers)
        cellList.addAll(id.tags)
    }
    try {
        val sheet = SheetsUtil.batchRead("1jnOGzo2GX3omiMcqxLRNACdYHYz3pkiYGiqbGFsHnXk", *cellList.toTypedArray())
        for (id in identifiers) {
            id.update(sheet)
        }
    } catch (e: Exception) {
        println("Failed to update identifers: ${e.message}")
    }
}

fun bufferedImage2Mat(image: BufferedImage): Mat {
    val byteArrayOutputStream = ByteArrayOutputStream()
    ImageIO.write(image, "jpg", byteArrayOutputStream)
    byteArrayOutputStream.flush()
    return Imgcodecs.imdecode(MatOfByte(*byteArrayOutputStream.toByteArray()), Imgcodecs.IMREAD_UNCHANGED)
}

fun mat2BufferedImage(matrix: Mat): BufferedImage {
    val mob = MatOfByte()
    Imgcodecs.imencode(".jpg", matrix, mob)
    return ImageIO.read(ByteArrayInputStream(mob.toArray()))
}

fun matchImg(img1: BufferedImage, img2: BufferedImage) {

    val mat = bufferedImage2Mat(img1)
    val matTemplate = bufferedImage2Mat(img2)

    val resultCols = mat.cols() - matTemplate.cols() + 1
    val resultRows = mat.rows() - matTemplate.rows() + 1
    if (resultCols > 0 && resultRows > 0) {
        val result = Mat(resultRows, resultCols, CvType.CV_8UC1)

        Imgproc.matchTemplate(mat, matTemplate, result, Imgproc.TM_SQDIFF_NORMED)
        val mmr = Core.minMaxLoc(result)

        if (mmr.minVal < 0.05) {
            println("match! ${mmr.minLoc}")
        } else {
            println("no match :(")
        }
    }
}

fun overlayText(under: String, text: String, position: Point, fontSize: Int = 100) {
    val composite = ImageMagickCmd("convert")
    val operation = IMOperation()
    operation.addImage(under)

    operation.fill("WHITE")
    operation.font("Gotham-Black.otf")
    operation.gravity("North")
    operation.pointsize(fontSize)
    operation.draw("\"text ${position.x},${position.y} '$text'\"")

    operation.addImage(under)
    composite.run(operation)
}

fun overlayImg(under: String, over: String, output: String, flip: Boolean = false) {
    val composite = ImageMagickCmd("convert")
    val operation = IMOperation()
    operation.compose("dstover")
    operation.addImage(over)
    if (flip) operation.flop()

    operation.openOperation()
    operation.addImage(under)

    operation.closeOperation()
    operation.composite()

    operation.addImage(output)
    composite.run(operation)
}

fun generateThumbnail(
    player1: String,
    character1: String,
    player2: String,
    character2: String,
    round: String,
    week: String,
    output: String
) {
    overlayImg("ThumbnailAssets\\thumbnailbg.jpg", character1, output)
    overlayImg(output, character2, output, true)
    overlayImg(output, "ThumbnailAssets\\overlay.png", output)
    overlayText(output, player1, Point(-480, 10))
    overlayText(output, player2, Point(480, 10))
    overlayText(output, round, Point(500, 970), 70)
    overlayText(output, week, Point(-500, 970), 70)
}

fun main(args: Array<String>) {
    ProcessStarter.setGlobalSearchPath(File("").absolutePath)
    System.load(File("opencv_java470.dll").absolutePath)
    generateThumbnail(
        "RAZZO1",
        "ThumbnailAssets\\May.png",
        "RAZZO2",
        "ThumbnailAssets\\Chipp.png",
        "WINNERS FINALS",
        "S1: WEEK #1",
        "test.png"
    )
    /*val cams = Webcam.getWebcams()
    for (cam in cams) {
        if (cam.name.contains("OBS-Camera")) {
            cam.open()
            val og = cam.image
            Thread {
                Thread.sleep(5000)
                var start = System.currentTimeMillis()
                val leo = ImageIO.read(File("leo.jpg"))
                print("attempting sub img leo: ")
                matchImg(og, leo)
                println("time: ${System.currentTimeMillis() - start}")
                start = System.currentTimeMillis()
                val chipp = ImageIO.read(File("chipp.jpg"))
                print("attempting sub img chipp: ")
                matchImg(og, chipp)
                println("time: ${System.currentTimeMillis() - start}")
                start = System.currentTimeMillis()
                val jacko = ImageIO.read(File("jacko.jpg"))
                print("attempting sub img jacko: ")
                matchImg(og, jacko)
                println("time: ${System.currentTimeMillis() - start}")
                start = System.currentTimeMillis()
            }.start()

            cam.close()
            break
        }
    }*/

    return
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