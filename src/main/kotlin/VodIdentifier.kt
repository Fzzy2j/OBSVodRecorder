import com.google.api.services.sheets.v4.model.BatchGetValuesResponse
import com.google.api.services.sheets.v4.model.ValueRange
import thumbnails.ThumbnailGenerator

class VodIdentifier(val mainIdentifiers: HashMap<String, String>, val tags: HashMap<String, String>, val thumbnailGenerator: ThumbnailGenerator? = null) {
    val sheetsValues = hashMapOf<String, String>()
    val oldSheetsValues = hashMapOf<String, String>()
    fun update(response: BatchGetValuesResponse) {
        for ((key, value) in sheetsValues) {
            oldSheetsValues[key] = value
        }
        for (v in response.values) {
            if (v !is ArrayList<*>) continue
            for (l in v) {
                val range = l as ValueRange
                for (va in range.values) {
                    if (va !is ArrayList<*>) continue
                    val values = va as List<List<String>>

                    val key = range.range.replace("'", "")
                    sheetsValues[key] = values[0][0]
                }
            }
        }
    }

    fun getValue(key: String): String? {
        val cell = mainIdentifiers[key]?: tags[key]?: return null
        return sheetsValues[cell]
    }

    fun getTagValues(): List<String> {
        val list = arrayListOf<String>()
        for (id in tags.values) {
            list.add(sheetsValues.getOrDefault(id, ""))
        }
        return list
    }

    fun getOldIdentifier(): String {
        val list = arrayListOf<String>()
        for (id in mainIdentifiers.values) {
            list.add(oldSheetsValues.getOrDefault(id, ""))
        }
        return list.joinToString(" ")
    }

    fun consumeChanges() {
        for ((key, value) in sheetsValues) {
            oldSheetsValues[key] = value
        }
    }

    fun anyChanges(): Boolean {
        for ((key, value) in sheetsValues) {
            if (!mainIdentifiers.contains(key)) continue
            if (!oldSheetsValues.containsKey(key)) return false
            if (value != oldSheetsValues[key]) return true
        }
        return false
    }
}