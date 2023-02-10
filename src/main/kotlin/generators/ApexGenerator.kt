package generators

import VodIdentifier
import util.ImgUtil
import util.YoutubeUtil
import java.awt.Point
import java.io.File

object ApexGenerator : VodIdentifier(
    "Apex",
    hashMapOf(
        "Game" to "Apex!A15",
        "ChampionSquad" to "Apex!A6"
    )
) {
    override fun getCurrentIdentifier(): String {
        return "${sheetsValues["Game"]} ${sheetsValues["ChampionSquad"]}"
    }

    override fun getOldIdentifier(): String {
        return "${oldSheetsValues["Game"]} ${oldSheetsValues["ChampionSquad"]}"
    }

    override fun vodFinished(file: File) {
        println("vod finished, generating thumbnail and uploading to youtube")

        val charsPicks = arrayListOf<String>()
        for (f in File("ThumbnailAssets\\Apex").listFiles()!!) {
            val name = f.nameWithoutExtension
            if (name.contains("chars")) {
                charsPicks.add(name)
            }
        }
        val pick = charsPicks.random()
        generateThumbnail(
            "ThumbnailAssets\\Apex\\$pick.png",
            "SEASON 6: WEEK #3",
            "GAME ${(oldSheetsValues["Game"] ?: return).uppercase()}",
            "${File(file.parent, file.nameWithoutExtension).absolutePath}.png"
        )

        val thumbnail = File(file.parent, "${file.nameWithoutExtension}.png")
        YoutubeUtil.uploadVideo(file.nameWithoutExtension, "description", arrayListOf("Apex"), file, thumbnail)
    }

    fun generateThumbnail(
        chars: String,
        smallText: String,
        bigText: String,
        output: String
    ) {
        ImgUtil.overlayImg("ThumbnailAssets\\Apex\\bg.png", chars, output)
        ImgUtil.overlayImg(output, "ThumbnailAssets\\Apex\\overlay.png", output)
        ImgUtil.overlayText(output, smallText, Point(100, 780), 30)
        ImgUtil.overlayText(output, bigText, Point(100, 800), 200)
    }
}