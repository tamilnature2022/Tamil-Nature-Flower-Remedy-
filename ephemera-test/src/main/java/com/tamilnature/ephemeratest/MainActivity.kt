// ADD THIS FUNCTION INSIDE MainActivity

private fun getLordStatus(
    lord: String,
    sign: Int
): String {

    val ownSigns = when (lord) {
        "சூரியன்" -> listOf(4)
        "சந்திரன்" -> listOf(3)
        "செவ்வாய்" -> listOf(0, 7)
        "புதன்" -> listOf(2, 5)
        "குரு" -> listOf(8, 11)
        "சுக்கிரன்" -> listOf(1, 6)
        "சனி" -> listOf(9, 10)
        else -> emptyList()
    }

    val exaltationSigns = mapOf(
        "சூரியன்" to 0,
        "சந்திரன்" to 1,
        "செவ்வாய்" to 9,
        "புதன்" to 5,
        "குரு" to 3,
        "சுக்கிரன்" to 11,
        "சனி" to 6
    )

    val debilitationSigns = mapOf(
        "சூரியன்" to 6,
        "சந்திரன்" to 7,
        "செவ்வாய்" to 3,
        "புதன்" to 11,
        "குரு" to 9,
        "சுக்கிரன்" to 5,
        "சனி" to 0
    )

    return when {
        sign in ownSigns ->
            "ஆட்சி (Own Sign)"

        exaltationSigns[lord] == sign ->
            "உச்சம் (Exaltation)"

        debilitationSigns[lord] == sign ->
            "நீசம் (Debilitation)"

        else ->
            "மற்ற ராசி (Other Sign)"
    }
}
