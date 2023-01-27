package thumbnails

import java.io.File

interface ThumbnailGenerator {
    fun vodFinished(file: File, values: HashMap<String, String>)
    fun onIdenfifierUpdate()
}