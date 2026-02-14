import java.time.Duration
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy")

fun escapeMd(value: String): String =
    value.replace("\\", "\\\\")
         .replace("|", "\\|")
         .replace("[", "\\[")
         .replace("]", "\\]")
         .replace("(", "\\(")
         .replace(")", "\\)")
         .replace("<", "&lt;")
         .replace(">", "&gt;")
         .replace("*", "\\*")
         .replace("_", "\\_")
         .replace("`", "\\`")
         .replace("#", "\\#")
         .replace("!", "\\!")

fun generateReport(
    today: ZonedDateTime,
    todayLocalDate: LocalDateTime,
    irelandFirstEntry: LocalDateTime,
    irpEntries: List<IrpEntry>,
    travelsList: List<Travel>,
    daysWithoutIRP: Long,
    irpDays: Long,
    lastIrpEnd: LocalDateTime,
    absentDays: Int,
    daysAccumulated: Long,
    daysLeft: Long,
    dayToApply: ZonedDateTime,
    goalDays: Int,
    excuseDays: Int
): String = buildString {
    appendLine("# Reckonable Residence Report")
    appendLine()
    appendLine("**Generated:** ${today.format(FORMATTER)}")
    appendLine()

    appendLine("## IRP Entries")
    appendLine()
    appendLine("| Name | Start | End |")
    appendLine("|------|-------|-----|")
    irpEntries.forEach {
        appendLine("| ${escapeMd(it.name)} | ${it.start.format(FORMATTER)} | ${it.end.format(FORMATTER)} |")
    }
    appendLine()

    appendLine("## Travel History")
    appendLine()
    appendLine("| Location | Departed | Returned | Days |")
    appendLine("|----------|----------|----------|------|")
    travelsList.forEach {
        appendLine("| ${escapeMd(it.location)} | ${it.start.format(FORMATTER)} | ${it.end.format(FORMATTER)} | ${daysBetween(it)} |")
    }
    appendLine()

    appendLine("## Year-by-Year Absence Breakdown")
    appendLine()
    appendLine("| Year | Period | Absent Days | After Excuse (${excuseDays}d) |")
    appendLine("|------|--------|-------------|-------------------------------|")
    var yrBegin = irelandFirstEntry.plusYears(1)
    var yrNum = 1
    var totalAbsentWithExcuses = 0
    while (yrBegin < todayLocalDate) {
        val yrAbsent = calculateAbsentDays(travelsList.filter {
            it.end >= yrBegin.minusYears(1) && it.end < yrBegin
        })
        val excused = maxOf(0, yrAbsent - excuseDays)
        appendLine("| $yrNum | ${yrBegin.minusYears(1).format(FORMATTER)} - ${yrBegin.format(FORMATTER)} | $yrAbsent | $excused |")
        totalAbsentWithExcuses += excused
        yrBegin = yrBegin.plusYears(1)
        yrNum++
    }
    val currentYrAbsent = calculateAbsentDays(travelsList.filter {
        it.end >= yrBegin.minusYears(1) && it.end < yrBegin
    })
    val currentExcused = maxOf(0, currentYrAbsent - excuseDays)
    totalAbsentWithExcuses += currentExcused
    appendLine("| $yrNum (current) | ${yrBegin.minusYears(1).format(FORMATTER)} - ${yrBegin.format(FORMATTER)} | $currentYrAbsent | $currentExcused |")
    appendLine()
    appendLine("**Remaining leave allowance this year:** ${excuseDays - currentYrAbsent} days")
    appendLine()

    val daysLeftWithExcuses = goalDays - (daysAccumulated - totalAbsentWithExcuses)
    val dayToApplyWithExcuses = today.plusDays(daysLeftWithExcuses)

    appendLine("## Summary")
    appendLine()
    appendLine("| Metric | Value |")
    appendLine("|--------|-------|")
    appendLine("| Goal | $goalDays days |")
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
    appendLine("| Goal without any absence | ${irelandFirstEntry.plusDays(goalDays.toLong()).format(FORMATTER)} |")
    appendLine("| Critical Year Starts | ${dayToApply.minusYears(1).format(FORMATTER)} |")
    appendLine("| **Earliest Application Date** | **${dayToApply.format(FORMATTER)}** |")
    appendLine("| Critical Year Starts (with excuses) | ${dayToApplyWithExcuses.minusYears(1).format(FORMATTER)} |")
    appendLine("| **Earliest Application Date (with excuses)** | **${dayToApplyWithExcuses.format(FORMATTER)}** |")
    appendLine("| IRP Renewal Date | ${lastIrpEnd.format(FORMATTER)} |")
}

fun mergeOverlappingIrpEntries(irpEntries: List<IrpEntry>): List<IrpEntry> {
    if (irpEntries.size <= 1) return irpEntries
    val sorted = irpEntries.sortedBy { it.start }
    val merged = mutableListOf(sorted.first())
    for (entry in sorted.drop(1)) {
        val last = merged.last()
        if (entry.start <= last.end) {
            merged[merged.lastIndex] = IrpEntry(
                name = last.name,
                start = last.start,
                end = maxOf(last.end, entry.end)
            )
        } else {
            merged.add(entry)
        }
    }
    return merged
}

fun mergeOverlappingTravels(travelsList: List<Travel>): List<Travel> {
    if (travelsList.size <= 1) return travelsList
    val sorted = travelsList.sortedBy { it.start }
    val merged = mutableListOf(sorted.first())
    for (travel in sorted.drop(1)) {
        val last = merged.last()
        if (travel.start <= last.end) {
            merged[merged.lastIndex] = Travel(
                start = last.start,
                end = maxOf(last.end, travel.end),
                location = last.location
            )
        } else {
            merged.add(travel)
        }
    }
    return merged
}

fun calculateAbsentDays(travelsList: List<Travel>): Int {
    return mergeOverlappingTravels(travelsList).sumOf { daysBetween(it) }.toInt()
}

data class DaysLeft(val raw: Long) {
    val clamped: Long get() = maxOf(0, raw)
}

fun calculateDaysLeft(goalDays: Int, daysAccumulated: Long, absentDays: Int): DaysLeft =
    DaysLeft(goalDays.toLong() - (daysAccumulated - absentDays))

// Both departure and return days count as reckonable residence (you're in Ireland on those days).
// Absent days are only the full days spent abroad, excluding both endpoints.
// e.g. Aug 10 to Aug 15: departure Aug 10 (reckonable), return Aug 15 (reckonable),
//      absent = Aug 11, 12, 13, 14 = 4 days = Duration(Aug 11, Aug 15) = 4
fun daysBetween(it: Travel) = maxOf(0, Duration.between(it.start.plusDays(1), it.end).toDays())
