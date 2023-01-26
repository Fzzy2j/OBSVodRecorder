package thumbnails

interface ThumbnailGenerator {
    fun vodFinished(identifiers: List<String>, tags: List<String>)
    fun onIdenfifierUpdate()
}