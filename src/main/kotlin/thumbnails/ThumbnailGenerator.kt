package thumbnails

interface ThumbnailGenerator {
    fun vodFinished(fileName: String, values: HashMap<String, String>)
    fun onIdenfifierUpdate()
}