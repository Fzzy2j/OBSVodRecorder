import com.google.api.services.sheets.v4.model.BatchGetValuesResponse
import com.google.api.services.sheets.v4.model.ValueRange

class VodIdentifier(val mainIdentifiers: List<String>, val tags: List<String>) {
    val sheetsValues = hashMapOf<String, String>()
    val oldSheetsValues = hashMapOf<String, String>()
    fun update(response: BatchGetValuesResponse) {
        for ((key, value) in sheetsValues) {
            oldSheetsValues[key] = value
        }
        for (v in response.values) {
            if (v !is ArrayList<*>) continue
            for (l in v){
                val range = l as ValueRange
                for (va in range.values) {
                    if (va !is ArrayList<*>) continue
                    val values = va as List<List<String>>

                    val key = range.range.replace("'", "")
                    sheetsValues[key] = values[0][0]
                    //values.forEach { it.forEach { cellValue -> identifierValues.add(cellValue) } }
                }
            }
        }
    }

    fun getIdentifier():String {
        val list = arrayListOf<String>()
        for (id in mainIdentifiers) {
            list.add(sheetsValues.getOrDefault(id, ""))
        }
        return list.joinToString(" ")
    }

    fun getOldIdentifier(): String {
        val list = arrayListOf<String>()
        for (id in mainIdentifiers) {
            list.add(oldSheetsValues.getOrDefault(id, ""))
        }
        return list.joinToString(" ")
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