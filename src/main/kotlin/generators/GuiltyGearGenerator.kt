package generators

import VodIdentifier
import com.github.sarxos.webcam.Webcam
import com.google.api.client.http.FileContent
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.Video
import com.google.api.services.youtube.model.VideoSnippet
import com.google.api.services.youtube.model.VideoStatus
import factory
import getCredentials
import paused
import threads
import transport
import util.ImgUtil
import util.ImgUtil.matchImg
import util.ImgUtil.overlayImg
import util.ImgUtil.overlayText
import util.YoutubeUtil
import java.awt.Point
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.File
import java.util.concurrent.Future
import javax.imageio.ImageIO

object GuiltyGearGenerator : VodIdentifier(
    "Guilty Gear",
    hashMapOf(
        "P1Name" to "Guilty Gear!B3",
        "P2Name" to "Guilty Gear!E3",
        "Round" to "Guilty Gear!H2",
        "BestOf" to "Guilty Gear!H3"
    )
) {

    var characterLeft = ""
    var characterRight = ""
    var characterLeftConfidence = 1.0
    var characterRightConfidence = 1.0
    override fun getCurrentIdentifier(): String {
        return "${sheetsValues["P1Name"]!!.replace(" (L)", "")} vs ${sheetsValues["P2Name"]!!.replace(" (L)", "")}"
    }

    override fun getOldIdentifier(): String {
        return "${oldSheetsValues["P1Name"]!!.replace(" (L)", "")} vs ${oldSheetsValues["P2Name"]!!.replace(" (L)", "")}"
    }

    override fun vodFinished(file: File) {
        println("vod finished, generating thumbnail and uploading to youtube")
        generateThumbnail(
            (oldSheetsValues["P1Name"] ?: return).uppercase(),
            "ThumbnailAssets\\GuiltyGear\\$characterLeft.png",
            (oldSheetsValues["P2Name"] ?: return).uppercase(),
            "ThumbnailAssets\\GuiltyGear\\$characterRight.png",
            (oldSheetsValues["Round"] ?: return).uppercase(),
            "S3: WEEK #8",
            "${File(file.parent, file.nameWithoutExtension).absolutePath}.png"
        )

        val thumbnail = File(file.parent, "${file.nameWithoutExtension}.png")
        val p1name = oldSheetsValues["P1Name"]!!.replace(" (L)", "").uppercase()
        val p2name = oldSheetsValues["P2Name"]!!.replace(" (L)", "").uppercase()
        YoutubeUtil.uploadVideo("S3W8 - $p1name vs $p2name", "#seriese #GuiltyGearStrive", arrayListOf("Guilty Gear"), file, thumbnail)

        characterLeftConfidence = 1.0
        characterRightConfidence = 1.0
    }

    var scanning = false

    var cam: Webcam? = null

    init {
        val cams = Webcam.getWebcams()
        for (c in cams) {
            if (c.name.contains("OBS-Camera")) {
                cam = c
                break
            }
        }
    }

    override fun onIdenfifierUpdate() {
        if (paused) {
            characterLeftConfidence = 1.0
            characterRightConfidence = 1.0
            return
        }
        if (scanning) return
        if (cam == null) return
        if (!cam!!.isOpen) cam!!.open()
        scanning = true
        try {
            var og = cam!!.image
            if (og == null) {
                cam!!.close()
                cam!!.open()
                og = cam!!.image
            }
            val list = arrayListOf<Future<*>>()

            class SearchResult(val character: String, val confidence: Double)

            val leftResults = arrayListOf<SearchResult>()
            val rightResults = arrayListOf<SearchResult>()
            for (file in File("CharacterSearches").listFiles()) {
                list.add(threads.submit {
                    try {
                        val img = ImageIO.read(file)
                        val at = AffineTransform()
                        at.concatenate(AffineTransform.getScaleInstance(-1.0, 1.0))
                        at.concatenate(AffineTransform.getTranslateInstance(-img.width.toDouble(), 0.0))
                        val flippedImg = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_RGB)
                        val g = flippedImg.createGraphics()
                        g.transform(at)
                        g.drawImage(img, 0, 0, null)
                        g.dispose()

                        for (i in arrayOf(img, flippedImg)) {
                            val result = matchImg(og, i)
                            if (result.side == ImgUtil.Side.LEFT) {
                                leftResults.add(SearchResult(file.nameWithoutExtension, result.value))
                            } else {
                                rightResults.add(SearchResult(file.nameWithoutExtension, result.value))
                            }
                        }
                    } catch (e: Exception) {
                        println("failed to match img: ${e.message}")
                        e.printStackTrace()
                    }
                })
            }
            list.forEach { it.get() }
            leftResults.sortBy { it.confidence }
            rightResults.sortBy { it.confidence }
            if (leftResults.isNotEmpty() || rightResults.isNotEmpty()) {
                if (leftResults[0].confidence < characterLeftConfidence) {
                    characterLeft = leftResults[0].character
                    characterLeftConfidence = leftResults[0].confidence
                }
                if (rightResults[0].confidence < characterRightConfidence) {
                    characterRight = rightResults[0].character
                    characterRightConfidence = rightResults[0].confidence
                }
            }
        } catch (e: Exception) {
            println("failed to scan: ${e.message}")
        }
        scanning = false
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
        overlayImg("ThumbnailAssets\\GuiltyGear\\thumbnailbg.jpg", character1, output)
        overlayImg(output, character2, output, true)
        overlayImg(output, "ThumbnailAssets\\GuiltyGear\\overlay.png", output)
        overlayText(output, player1, Point(-480, 10))
        overlayText(output, player2, Point(480, 10))
        overlayText(output, round, Point(500, 970), 70)
        overlayText(output, week, Point(-500, 970), 70)
    }
}