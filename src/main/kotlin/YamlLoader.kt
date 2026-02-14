import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor

private val DATE_REGEX = Regex("""\d{4}-\d{2}-\d{2}""")
private val IRP_NAME_REGEX = Regex("""[a-zA-Z0-9_\- ]+""")

fun loadIrpInfo(filePath: String): IrpInfo {
    val file = File(filePath)
    require(file.exists()) { "IRP info file not found: $filePath" }

    val yaml = Yaml(SafeConstructor(LoaderOptions()))
    val data = yaml.load<Any>(file.reader())
    require(data is Map<*, *>) { "IRP info file must contain a YAML mapping at the top level." }

    val allowedKeys = setOf("ireland_first_entry", "irp")
    val unexpectedKeys = data.keys.filterNot { it in allowedKeys }
    require(unexpectedKeys.isEmpty()) { "Unexpected keys in IRP info file: $unexpectedKeys. Allowed keys: $allowedKeys" }

    require(data.containsKey("ireland_first_entry")) { "Missing required key 'ireland_first_entry' in $filePath" }
    val irelandFirstEntry = parseDate(data["ireland_first_entry"]!!)

    require(data.containsKey("irp")) { "Missing required key 'irp' in $filePath" }
    val irpRaw = data["irp"]
    require(irpRaw is List<*>) { "'irp' must be a list of entries in $filePath" }
    require(irpRaw.isNotEmpty()) { "'irp' list must not be empty in $filePath" }

    val irpEntries = irpRaw.mapIndexed { index, entry ->
        require(entry is Map<*, *>) { "IRP entry at index $index must be a mapping." }

        val entryAllowedKeys = setOf("name", "start", "end")
        val entryUnexpectedKeys = entry.keys.filterNot { it in entryAllowedKeys }
        require(entryUnexpectedKeys.isEmpty()) { "Unexpected keys in IRP entry at index $index: $entryUnexpectedKeys. Allowed keys: $entryAllowedKeys" }

        require(entry.containsKey("name")) { "IRP entry at index $index is missing 'name'." }
        require(entry.containsKey("start")) { "IRP entry at index $index is missing 'start'." }
        require(entry.containsKey("end")) { "IRP entry at index $index is missing 'end'." }

        val name = entry["name"].toString()
        require(IRP_NAME_REGEX.matches(name)) { "IRP entry name at index $index contains invalid characters: '$name'. Only alphanumeric, spaces, hyphens, and underscores are allowed." }

        val start = parseDate(entry["start"]!!)
        val end = parseDate(entry["end"]!!)
        require(end >= start) { "IRP entry '$name' has end date before start date." }

        IrpEntry(name = name, start = start, end = end)
    }
    return IrpInfo(irelandFirstEntry, irpEntries)
}

fun parseDate(value: Any): LocalDateTime {
    val str = when (value) {
        is java.util.Date -> value.toInstant().atZone(ZoneId.of("GMT")).toLocalDate().toString()
        else -> value.toString().trim()
    }
    require(DATE_REGEX.matches(str)) { "Invalid date format: '$str'. Expected yyyy-MM-dd." }
    return LocalDate.parse(str).atStartOfDay()
}
