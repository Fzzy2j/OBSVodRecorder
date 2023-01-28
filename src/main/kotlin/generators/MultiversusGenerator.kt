package generators

import VodIdentifier

object MultiversusGenerator : VodIdentifier(
    hashMapOf(
        "P1Name" to "Multiversus!B3",
        "P2Name" to "Multiversus!E3",
        "Round" to "Multiversus!H2",
        "BestOf" to "Multiversus!H3"
    )
) {
    override fun getCurrentIdentifier(): String {
        return "${sheetsValues["P1Name"]} vs ${sheetsValues["P2Name"]}"
    }

    override fun getOldIdentifier(): String {
        return "${oldSheetsValues["P1Name"]} vs ${oldSheetsValues["P2Name"]}"
    }
}