package util

import com.google.api.client.http.FileContent
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.Video
import com.google.api.services.youtube.model.VideoAgeGating
import com.google.api.services.youtube.model.VideoSnippet
import com.google.api.services.youtube.model.VideoStatus
import factory
import getCredentials
import transport
import java.io.File
import kotlin.math.roundToInt

object YoutubeUtil {

    fun uploadVideo(title: String, description: String, tags: List<String>, file: File, thumbnail: File) {

        val youtubeService =
            YouTube.Builder(transport, factory, getCredentials(transport)).setApplicationName("FzzyApexGraphics")
                .build()

        val ve = Video()
        ve.snippet = VideoSnippet()
        ve.snippet.title = title
        ve.snippet.description = description
        ve.snippet.tags = tags

        ve.status = VideoStatus()
        ve.status.selfDeclaredMadeForKids = false
        ve.status.privacyStatus = "unlisted"

        val stream = FileContent("video/mp4", file)
        val insert = youtubeService.Videos().insert("snippet,status", ve, stream)
        insert.mediaHttpUploader.chunkSize = 512 * 1024 * 1024 // 512MB
        insert.mediaHttpUploader.setProgressListener {
            println("video - $title: ${(it.progress * 100).roundToInt()}%")
        }
        val video = insert.execute()

        val thumbnailStream = FileContent("image/png", thumbnail)
        val thumbnailSet = youtubeService.Thumbnails().set(video.id, thumbnailStream)
        thumbnailSet.mediaHttpUploader.setProgressListener {
            println("thumbnail - $title: ${(it.progress * 100).roundToInt()}%")
        }
        thumbnailSet.execute()
    }

}