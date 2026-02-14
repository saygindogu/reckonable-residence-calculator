import java.io.File

private val LOCATION_REGEX = Regex("""[a-zA-Z0-9_\- ]+""")

fun loadTravelsFromCsv(filePath: String): List<Travel> {
    val file = File(filePath)
    require(file.exists()) { "Travels CSV file not found: $filePath" }

    val lines = file.readLines()
    require(lines.isNotEmpty()) { "Travels CSV file is empty: $filePath" }

    val header = lines.first().trim()
    require(header == "start,end,location") { "Unexpected CSV header in $filePath: '$header'. Expected: 'start,end,location'" }

    return lines.drop(1)
            .filter { it.isNotBlank() }
            .mapIndexed { index, line ->
                val parts = line.split(",")
                require(parts.size == 3) { "CSV row ${index + 2} has ${parts.size} columns, expected 3: '$line'" }

                val location = parts[2].trim()
                require(location.isNotEmpty()) { "CSV row ${index + 2} has an empty location." }
                require(LOCATION_REGEX.matches(location)) { "CSV row ${index + 2} has invalid location: '$location'. Only alphanumeric, spaces, hyphens, and underscores are allowed." }

                val start = parseDate(parts[0].trim())
                val end = parseDate(parts[1].trim())
                require(end >= start) { "CSV row ${index + 2} has end date before start date." }

                Travel(start, end, location)
            }
}
