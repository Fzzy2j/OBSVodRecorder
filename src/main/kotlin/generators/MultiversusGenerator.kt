package generators

import VodIdentifier
import getCurrentWeek
import util.ImgUtil
import util.YoutubeUtil
import java.awt.Point
import java.io.File

object MultiversusGenerator : VodIdentifier(
    "Multiversus",
    hashMapOf(
        "P1Name" to "Multiversus!B3",
        "P2Name" to "Multiversus!E3",
        "Round" to "Multiversus!H2",
        "BestOf" to "Multiversus!H3"
    )
) {
    override fun getCurrentIdentifier(): String {
        return "${sheetsValues["P1Name"]!!.replace(" (L)", "")} vs ${sheetsValues["P2Name"]!!.replace(" (L)", "")}"
    }

    override fun getOldIdentifier(): String {
        return "${oldSheetsValues["P1Name"]!!.replace(" (L)", "")} vs ${oldSheetsValues["P2Name"]!!.replace(" (L)", "")}"
    }

    override fun vodFinished(file: File) {
        println("vod finished, generating thumbnail and uploading to youtube")

        val charsPicks = arrayListOf<String>()
        for (f in File("ThumbnailAssets\\Multiversus").listFiles()!!) {
            val name = f.nameWithoutExtension
            if (name.contains("chars")) {
                charsPicks.add(name)
            }
        }
        val pick = charsPicks.random()
        val week = getCurrentWeek(file.parentFile)
        val round = (oldSheetsValues["Round"]?: return).uppercase().replace(" RESET", "")
        generateThumbnail(
            "ThumbnailAssets\\Multiversus\\$pick.png",
            "SEASON 1: WEEK #$week",
            round,
            "${File(file.parent, file.nameWithoutExtension).absolutePath}.png"
        )

        val thumbnail = File(file.parent, "${file.nameWithoutExtension}.png")
        val p1name = oldSheetsValues["P1Name"]!!.replace(" (L)", "").uppercase()
        val p2name = oldSheetsValues["P2Name"]!!.replace(" (L)", "").uppercase()
        YoutubeUtil.uploadVideo(
            "S1W$week - $p1name vs $p2name",
            "#seriese #Multiversus", arrayListOf("Multiversus"),
            file,
            thumbnail
        )
    }

    fun generateThumbnail(
        chars: String,
        smallText: String,
        bigText: String,
        output: String
    ) {
        ImgUtil.overlayImg("ThumbnailAssets\\Multiversus\\bg.png", chars, output)
        ImgUtil.overlayImg(output, "ThumbnailAssets\\Multiversus\\overlay.png", output)
        ImgUtil.overlayText(output, smallText, Point(100, 780), 30)
        ImgUtil.overlayText(output, bigText, Point(100, 800), 200)
    }
}