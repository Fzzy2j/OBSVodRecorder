import com.google.api.services.sheets.v4.model.BatchGetValuesResponse
import com.google.api.services.sheets.v4.model.ClearValuesRequest
import com.google.api.services.sheets.v4.model.ValueRange

object SheetsUtil {

    fun clearSheet(sheetId: String, sheetName: String) {
        googleSheetService.spreadsheets().values().clear(sheetId, sheetName, ClearValuesRequest()).execute()
    }

    fun writeToSheet(
        sheetId: String,
        sheetName: String,
        content: List<List<String>>,
        offsetX: Int = 0,
        offsetY: Int = 0,
        vertical: Boolean = true
    ) {
        val body = ValueRange().setMajorDimension(if (vertical) "COLUMNS" else "ROWS").setValues(content)
        val startXChar = (offsetX + 65).toChar()
        googleSheetService.spreadsheets().values().update(sheetId, "$sheetName!$startXChar${offsetY + 1}", body)
            .setValueInputOption("USER_ENTERED").execute()
    }

    fun readFromSheet(sheetId: String, range: String): List<List<String>> {
        val received = googleSheetService.spreadsheets().values().get(sheetId, range).execute()
        return received.getValues() as List<List<String>>
    }

    fun batchRead(sheetId: String, vararg ranges: String): BatchGetValuesResponse {
        val request = googleSheetService.spreadsheets().values().batchGet(sheetId)
        request.ranges = ranges.asList()
        return request.execute()
    }
}