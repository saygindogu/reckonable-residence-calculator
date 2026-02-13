import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.yaml.snakeyaml.Yaml

val EXCUSE_DAYS = 70

fun main() {
    val FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy")
    val GOAL_DAYS = 1826

    val irpInfo = loadIrpInfo("irp_info.yaml")
    val irelandFirstEntry = irpInfo.irelandFirstEntry
    val irpEntries = irpInfo.irpEntries

    val today = Instant.now().atZone(ZoneId.of("GMT"))
    val todayLocalDate = today.toLocalDateTime()

    // Note that this requires additional authorization from the Irish Government to be counted as reckonable residence.
    // Please refer to the documents issued by Department of Justice with relation to pandemic and residence permits.
    val daysWithoutIRP = Duration.between(irelandFirstEntry, irpEntries.first().start).toDays()
    val irpDays = irpEntries.sumOf { entry ->
        val effectiveEnd = minOf(entry.end, todayLocalDate)
        Duration.between(entry.start, effectiveEnd).toDays()
    }
    val lastIrpEnd = irpEntries.last().end

    val travelsList = loadTravelsFromCsv("travels.csv")

    val absentDays = calculateAbsentDays(travelsList)
    val daysAccumulated = daysWithoutIRP + irpDays
    val daysLeft = GOAL_DAYS - (daysAccumulated - absentDays)
    val dayToApply = today.plusDays(daysLeft)

    val dateStamp = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    val outDir = File("out").apply { mkdirs() }
    val outFile = File(outDir, "${dateStamp}_reckonable_residence_output.md")

    val md = buildString {
        appendLine("# Reckonable Residence Report")
        appendLine()
        appendLine("**Generated:** ${today.format(FORMATTER)}")
        appendLine()

        appendLine("## IRP Entries")
        appendLine()
        appendLine("| Name | Start | End |")
        appendLine("|------|-------|-----|")
        irpEntries.forEach {
            appendLine("| ${it.name} | ${it.start.format(FORMATTER)} | ${it.end.format(FORMATTER)} |")
        }
        appendLine()

        appendLine("## Travel History")
        appendLine()
        appendLine("| Location | Departed | Returned | Days |")
        appendLine("|----------|----------|----------|------|")
        travelsList.forEach {
            appendLine("| ${it.location} | ${it.start.format(FORMATTER)} | ${it.end.format(FORMATTER)} | ${daysBetween(it)} |")
        }
        appendLine()

        appendLine("## Year-by-Year Absence Breakdown")
        appendLine()
        appendLine("| Year | Period | Absent Days | After Excuse (${EXCUSE_DAYS}d) |")
        appendLine("|------|--------|-------------|-------------------------------|")
        var yrBegin = irelandFirstEntry.plusYears(1)
        var yrNum = 1
        var totalAbsentWithExcuses = 0
        while (yrBegin < todayLocalDate) {
            val yrAbsent = calculateAbsentDays(travelsList.filter {
                it.end >= yrBegin.minusYears(1) && it.end < yrBegin
            })
            val excused = maxOf(0, yrAbsent - EXCUSE_DAYS)
            appendLine("| $yrNum | ${yrBegin.minusYears(1).format(FORMATTER)} - ${yrBegin.format(FORMATTER)} | $yrAbsent | $excused |")
            totalAbsentWithExcuses += excused
            yrBegin = yrBegin.plusYears(1)
            yrNum++
        }
        val currentYrAbsent = calculateAbsentDays(travelsList.filter {
            it.end >= yrBegin.minusYears(1) && it.end < yrBegin
        })
        val currentExcused = maxOf(0, currentYrAbsent - EXCUSE_DAYS)
        totalAbsentWithExcuses += currentExcused
        appendLine("| $yrNum (current) | ${yrBegin.minusYears(1).format(FORMATTER)} - ${yrBegin.format(FORMATTER)} | $currentYrAbsent | $currentExcused |")
        appendLine()
        appendLine("**Remaining leave allowance this year:** ${EXCUSE_DAYS - currentYrAbsent} days")
        appendLine()

        val daysLeftWithExcuses = GOAL_DAYS - (daysAccumulated - totalAbsentWithExcuses)
        val dayToApplyWithExcuses = today.plusDays(daysLeftWithExcuses)

        appendLine("## Summary")
        appendLine()
        appendLine("| Metric | Value |")
        appendLine("|--------|-------|")
        appendLine("| Goal | $GOAL_DAYS days |")
        appendLine("| Days Accumulated | $daysAccumulated |")
        appendLine("| Days in Ireland without Stamp | $daysWithoutIRP |")
        appendLine("| IRP Days | $irpDays |")
        appendLine("| Total Absent Days | $absentDays |")
        appendLine("| Total Absent (after excuses) | $totalAbsentWithExcuses |")
        appendLine("| Days Left | $daysLeft |")
        appendLine("| Days Left (with excuses) | $daysLeftWithExcuses |")
        appendLine()

        appendLine("## Key Dates")
        appendLine()
        appendLine("| | Date |")
        appendLine("|--|------|")
        appendLine("| Goal without any absence | ${irelandFirstEntry.plusDays(GOAL_DAYS.toLong()).format(FORMATTER)} |")
        appendLine("| Critical Year Starts | ${dayToApply.minusYears(1).format(FORMATTER)} |")
        appendLine("| **Earliest Application Date** | **${dayToApply.format(FORMATTER)}** |")
        appendLine("| Critical Year Starts (with excuses) | ${dayToApplyWithExcuses.minusYears(1).format(FORMATTER)} |")
        appendLine("| **Earliest Application Date (with excuses)** | **${dayToApplyWithExcuses.format(FORMATTER)}** |")
        appendLine("| IRP Renewal Date | ${lastIrpEnd.format(FORMATTER)} |")
    }

    outFile.writeText(md)
    println("Report written to ${outFile.path}")
}

fun calculateAbsentDays(travelsList: List<Travel>): Int {
    return travelsList.sumOf { daysBetween(it) }.toInt()
}

// Travel dates count into reckonable residence.
// Start Inclusive, so we need to add 1 day more. End exclusive so we don't add anything.
private fun daysBetween(it: Travel) = Duration.between(it.start.plusDays(1), it.end).toDays()

private fun parseDate(value: Any): LocalDateTime = when (value) {
    is java.util.Date -> value.toInstant().atZone(ZoneId.of("GMT")).toLocalDate().atStartOfDay()
    else -> LocalDate.parse(value.toString().trim()).atStartOfDay()
}

@Suppress("UNCHECKED_CAST")
private fun loadIrpInfo(filePath: String): IrpInfo {
    val yaml = Yaml()
    val data = yaml.load<Map<String, Any>>(File(filePath).reader())
    val irelandFirstEntry = parseDate(data["ireland_first_entry"]!!)
    val irpList = data["irp"] as List<Map<String, Any>>
    val irpEntries = irpList.map { entry ->
        IrpEntry(
                name = entry["name"].toString(),
                start = parseDate(entry["start"]!!),
                end = parseDate(entry["end"]!!)
        )
    }
    return IrpInfo(irelandFirstEntry, irpEntries)
}

private fun loadTravelsFromCsv(filePath: String): List<Travel> {
    return File(filePath).readLines()
            .drop(1) // skip header
            .filter { it.isNotBlank() }
            .map { line ->
                val parts = line.split(",")
                Travel(
                        parseDate(parts[0].trim()),
                        parseDate(parts[1].trim()),
                        parts[2].trim()
                )
            }
}
