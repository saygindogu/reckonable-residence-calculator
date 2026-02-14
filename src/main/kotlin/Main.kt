import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

val EXCUSE_DAYS = 70

fun main() {
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

    val md = generateReport(
        today = today,
        todayLocalDate = todayLocalDate,
        irelandFirstEntry = irelandFirstEntry,
        irpEntries = irpEntries,
        travelsList = travelsList,
        daysWithoutIRP = daysWithoutIRP,
        irpDays = irpDays,
        lastIrpEnd = lastIrpEnd,
        absentDays = absentDays,
        daysAccumulated = daysAccumulated,
        daysLeft = daysLeft,
        dayToApply = dayToApply,
        goalDays = GOAL_DAYS,
        excuseDays = EXCUSE_DAYS
    )

    outFile.writeText(md)
    println("Report written to ${outFile.path}")
}
