package thumbnails

import com.github.sarxos.webcam.Webcam
import threads
import util.ImgUtil
import util.ImgUtil.matchImg
import util.ImgUtil.overlayImg
import util.ImgUtil.overlayText
import java.awt.Point
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.File
import java.util.concurrent.Future
import javax.imageio.ImageIO

object GuiltyGearGenerator : ThumbnailGenerator {

    var characterLeft = ""
    var characterRight = ""
    var characterLeftConfidence = 1.0
    var characterRightConfidence = 1.0

    override fun vodFinished(fileName: String, values: HashMap<String, String>) {
        generateThumbnail(
            values["P1Name"] ?: return,
            "ThumbnailAssets\\$characterLeft.png",
            values["P2Name"] ?: return,
            "ThumbnailAssets\\$characterRight.png",
            (values["Round"] ?: return).uppercase(),
            "S1: WEEK #1",
            "$fileName.png"
        )
        characterLeftConfidence = 1.0
        characterRightConfidence = 1.0
    }

    var scanning = false

    override fun onIdenfifierUpdate() {
        if (scanning) return
        scanning = true
        val cams = Webcam.getWebcams()
        for (cam in cams) {
            if (cam.name.contains("OBS-Camera")) {
                cam.open()
                val og = cam.image
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
                    println(characterLeft)
                    println(characterRight)
                }

                cam.close()
                break
            }
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
        overlayImg("ThumbnailAssets\\thumbnailbg.jpg", character1, output)
        overlayImg(output, character2, output, true)
        overlayImg(output, "ThumbnailAssets\\overlay.png", output)
        overlayText(output, player1, Point(-480, 10))
        overlayText(output, player2, Point(480, 10))
        overlayText(output, round, Point(500, 970), 70)
        overlayText(output, week, Point(-500, 970), 70)
    }
}