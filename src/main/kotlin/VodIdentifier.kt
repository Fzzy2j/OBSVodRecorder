import com.google.api.services.sheets.v4.model.BatchGetValuesResponse
import com.google.api.services.sheets.v4.model.ValueRange
import java.io.File

abstract class VodIdentifier(val sheetsCells: HashMap<String, String>) {

    abstract fun getCurrentIdentifier(): String
    abstract fun getOldIdentifier(): String
    open fun vodFinished(file: File) {}
    open fun onIdenfifierUpdate() {}

    val sheetsValues = hashMapOf<String, String>()
    val oldSheetsValues = hashMapOf<String, String>()
    fun update(response: BatchGetValuesResponse) {
        consumeChanges()
        for (v in response.values) {
            if (v !is ArrayList<*>) continue
            for (l in v) {
                val range = l as ValueRange
                for (va in range.values) {
                    if (va !is ArrayList<*>) continue
                    val values = va as List<List<String>>

                    val key = range.range.replace("'", "")
                    for ((cellKey, cellRange) in sheetsCells) {
                        if (cellRange == key) {
                            sheetsValues[cellKey] = values[0][0]
                            break
                        }
                    }
                }
            }
        }
    }

    fun consumeChanges() {
        oldSheetsValues.clear()
        for ((key, value) in sheetsValues) {
            oldSheetsValues[key] = value
        }
    }

    fun anyChanges(): Boolean {
        if (oldSheetsValues.isEmpty()) return false
        return getCurrentIdentifier() != getOldIdentifier()
    }
}