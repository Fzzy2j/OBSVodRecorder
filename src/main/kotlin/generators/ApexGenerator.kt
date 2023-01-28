package generators

import VodIdentifier

object ApexGenerator : VodIdentifier(
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
}