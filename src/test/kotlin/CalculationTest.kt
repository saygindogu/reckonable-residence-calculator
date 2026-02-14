import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class CalculationTest {

    private fun dt(year: Int, month: Int, day: Int): LocalDateTime =
        LocalDateTime.of(year, month, day, 0, 0)

    @Test
    fun `daysBetween excludes both departure and return days`() {
        // Travel from Aug 10 to Aug 15
        // Aug 10 (departure) and Aug 15 (return) both count as reckonable (in Ireland).
        // Absent days = Aug 11, 12, 13, 14 = 4 days
        val travel = Travel(dt(2020, 8, 10), dt(2020, 8, 15), "TR")
        assertEquals(4, daysBetween(travel))
    }

    @Test
    fun `daysBetween for same day travel is zero`() {
        // Departed and returned same day - clamped to 0
        val travel = Travel(dt(2020, 8, 10), dt(2020, 8, 10), "TR")
        assertEquals(0, daysBetween(travel))
    }

    @Test
    fun `daysBetween for next day return is zero`() {
        // Departed Aug 10, returned Aug 11 -> both are travel days, 0 absent days
        val travel = Travel(dt(2020, 8, 10), dt(2020, 8, 11), "TR")
        assertEquals(0, daysBetween(travel))
    }

    @Test
    fun `daysBetween for two day gap is one`() {
        // Departed Aug 10, returned Aug 12
        // Aug 10 (reckonable), Aug 11 (absent), Aug 12 (reckonable) = 1 absent day
        val travel = Travel(dt(2020, 8, 10), dt(2020, 8, 12), "TR")
        assertEquals(1, daysBetween(travel))
    }

    @Test
    fun `calculateAbsentDays sums multiple travels`() {
        val travels = listOf(
            Travel(dt(2020, 8, 10), dt(2020, 8, 25), "TR"),  // 14 days
            Travel(dt(2021, 3, 15), dt(2021, 3, 22), "UK"),  // 6 days
        )
        assertEquals(20, calculateAbsentDays(travels))
    }

    @Test
    fun `calculateAbsentDays with empty list returns zero`() {
        assertEquals(0, calculateAbsentDays(emptyList()))
    }

    @Test
    fun `calculateAbsentDays with single travel`() {
        val travels = listOf(
            Travel(dt(2020, 8, 10), dt(2020, 8, 25), "TR")  // 14 days absent
        )
        assertEquals(14, calculateAbsentDays(travels))
    }

    @Test
    fun `calculateAbsentDays with same day travels does not go negative`() {
        val travels = listOf(
            Travel(dt(2020, 8, 10), dt(2020, 8, 10), "TR"),  // 0 days (clamped)
            Travel(dt(2020, 9, 1), dt(2020, 9, 1), "UK"),    // 0 days (clamped)
        )
        assertEquals(0, calculateAbsentDays(travels))
    }

    @Test
    fun `calculateAbsentDays with mix of same day and multi day travels`() {
        val travels = listOf(
            Travel(dt(2020, 8, 10), dt(2020, 8, 10), "TR"),  // 0 days (clamped)
            Travel(dt(2021, 3, 15), dt(2021, 3, 22), "UK"),  // 6 days
        )
        assertEquals(6, calculateAbsentDays(travels))
    }

    @Test
    fun `calculateAbsentDays with next day returns`() {
        val travels = listOf(
            Travel(dt(2020, 8, 10), dt(2020, 8, 11), "TR"),  // 0 days
            Travel(dt(2020, 9, 1), dt(2020, 9, 2), "UK"),    // 0 days
        )
        assertEquals(0, calculateAbsentDays(travels))
    }

    @Test
    fun `calculateAbsentDays with unsorted travels returns same result as sorted`() {
        val unsorted = listOf(
            Travel(dt(2021, 3, 15), dt(2021, 3, 22), "UK"),  // 6 days
            Travel(dt(2020, 8, 10), dt(2020, 8, 25), "TR"),  // 14 days
            Travel(dt(2021, 12, 20), dt(2022, 1, 5), "DE"),  // 15 days
        )
        val sorted = listOf(
            Travel(dt(2020, 8, 10), dt(2020, 8, 25), "TR"),
            Travel(dt(2021, 3, 15), dt(2021, 3, 22), "UK"),
            Travel(dt(2021, 12, 20), dt(2022, 1, 5), "DE"),
        )
        assertEquals(calculateAbsentDays(sorted), calculateAbsentDays(unsorted))
        assertEquals(35, calculateAbsentDays(unsorted))
    }

    @Test
    fun `mergeOverlappingTravels with no overlaps returns same list`() {
        val travels = listOf(
            Travel(dt(2020, 8, 10), dt(2020, 8, 25), "TR"),
            Travel(dt(2021, 3, 15), dt(2021, 3, 22), "UK"),
        )
        val merged = mergeOverlappingTravels(travels)
        assertEquals(2, merged.size)
    }

    @Test
    fun `mergeOverlappingTravels merges fully overlapping intervals`() {
        val travels = listOf(
            Travel(dt(2020, 8, 10), dt(2020, 8, 25), "TR"),
            Travel(dt(2020, 8, 12), dt(2020, 8, 20), "UK"),  // fully inside first
        )
        val merged = mergeOverlappingTravels(travels)
        assertEquals(1, merged.size)
        assertEquals(dt(2020, 8, 10), merged[0].start)
        assertEquals(dt(2020, 8, 25), merged[0].end)
    }

    @Test
    fun `mergeOverlappingTravels merges partially overlapping intervals`() {
        val travels = listOf(
            Travel(dt(2020, 8, 10), dt(2020, 8, 20), "TR"),
            Travel(dt(2020, 8, 15), dt(2020, 8, 25), "UK"),  // overlaps with first
        )
        val merged = mergeOverlappingTravels(travels)
        assertEquals(1, merged.size)
        assertEquals(dt(2020, 8, 10), merged[0].start)
        assertEquals(dt(2020, 8, 25), merged[0].end)
    }

    @Test
    fun `mergeOverlappingTravels merges adjacent intervals sharing a boundary`() {
        val travels = listOf(
            Travel(dt(2020, 8, 10), dt(2020, 8, 20), "TR"),
            Travel(dt(2020, 8, 20), dt(2020, 8, 25), "UK"),  // starts where first ends
        )
        val merged = mergeOverlappingTravels(travels)
        assertEquals(1, merged.size)
        assertEquals(dt(2020, 8, 10), merged[0].start)
        assertEquals(dt(2020, 8, 25), merged[0].end)
    }

    @Test
    fun `mergeOverlappingTravels handles unsorted input`() {
        val travels = listOf(
            Travel(dt(2020, 8, 15), dt(2020, 8, 25), "UK"),
            Travel(dt(2020, 8, 10), dt(2020, 8, 20), "TR"),
        )
        val merged = mergeOverlappingTravels(travels)
        assertEquals(1, merged.size)
        assertEquals(dt(2020, 8, 10), merged[0].start)
        assertEquals(dt(2020, 8, 25), merged[0].end)
    }

    @Test
    fun `mergeOverlappingTravels with empty list returns empty`() {
        assertEquals(0, mergeOverlappingTravels(emptyList()).size)
    }

    @Test
    fun `mergeOverlappingTravels with single item returns same`() {
        val travels = listOf(Travel(dt(2020, 8, 10), dt(2020, 8, 25), "TR"))
        val merged = mergeOverlappingTravels(travels)
        assertEquals(1, merged.size)
    }

    @Test
    fun `mergeOverlappingTravels merges chain of overlaps`() {
        val travels = listOf(
            Travel(dt(2020, 8, 10), dt(2020, 8, 15), "TR"),
            Travel(dt(2020, 8, 14), dt(2020, 8, 20), "UK"),
            Travel(dt(2020, 8, 19), dt(2020, 8, 25), "DE"),
        )
        val merged = mergeOverlappingTravels(travels)
        assertEquals(1, merged.size)
        assertEquals(dt(2020, 8, 10), merged[0].start)
        assertEquals(dt(2020, 8, 25), merged[0].end)
    }

    @Test
    fun `calculateAbsentDays does not double count overlapping travels`() {
        // Aug 10 to Aug 25 = 14 absent days (without overlap)
        // Aug 15 to Aug 20 is fully inside the first trip
        val travels = listOf(
            Travel(dt(2020, 8, 10), dt(2020, 8, 25), "TR"),
            Travel(dt(2020, 8, 15), dt(2020, 8, 20), "UK"),
        )
        // Without merging this would be 14 + 4 = 18. With merging it should be 14.
        assertEquals(14, calculateAbsentDays(travels))
    }

    @Test
    fun `calculateDaysLeft raw returns positive when goal not yet reached`() {
        // 1826 - (1600 - 14) = 240
        val result = calculateDaysLeft(1826, 1600, 14)
        assertEquals(240, result.raw)
        assertEquals(240, result.clamped)
    }

    @Test
    fun `calculateDaysLeft clamped returns zero when accumulated exceeds goal`() {
        // 1826 - (2000 - 50) = -124
        val result = calculateDaysLeft(1826, 2000, 50)
        assertEquals(-124, result.raw)
        assertEquals(0, result.clamped)
    }

    @Test
    fun `calculateDaysLeft returns zero when exactly at goal`() {
        // 1826 - (1840 - 14) = 0
        val result = calculateDaysLeft(1826, 1840, 14)
        assertEquals(0, result.raw)
        assertEquals(0, result.clamped)
    }

    @Test
    fun `calculateDaysLeft with no absences`() {
        // 1826 - (1500 - 0) = 326
        val result = calculateDaysLeft(1826, 1500, 0)
        assertEquals(326, result.raw)
        assertEquals(326, result.clamped)
    }

    @Test
    fun `calculateDaysLeft raw is negative when goal exceeded`() {
        // 1826 - (1900 - 0) = -74
        val result = calculateDaysLeft(1826, 1900, 0)
        assertEquals(-74, result.raw)
        assertEquals(0, result.clamped)
    }

    @Test
    fun `mergeOverlappingIrpEntries merges overlapping entries`() {
        val entries = listOf(
            IrpEntry("stamp1-1", dt(2021, 6, 17), dt(2022, 6, 17)),
            IrpEntry("stamp1-2", dt(2021, 6, 10), dt(2022, 11, 1)),
        )
        val merged = mergeOverlappingIrpEntries(entries)
        assertEquals(1, merged.size)
        assertEquals(dt(2021, 6, 10), merged[0].start)
        assertEquals(dt(2022, 11, 1), merged[0].end)
    }

    @Test
    fun `mergeOverlappingIrpEntries merges adjacent entries sharing a boundary`() {
        val entries = listOf(
            IrpEntry("a", dt(2021, 6, 10), dt(2022, 11, 1)),
            IrpEntry("b", dt(2022, 10, 26), dt(2026, 10, 26)),
        )
        val merged = mergeOverlappingIrpEntries(entries)
        assertEquals(1, merged.size)
        assertEquals(dt(2021, 6, 10), merged[0].start)
        assertEquals(dt(2026, 10, 26), merged[0].end)
    }

    @Test
    fun `mergeOverlappingIrpEntries keeps non-overlapping entries separate`() {
        val entries = listOf(
            IrpEntry("a", dt(2020, 1, 1), dt(2020, 6, 1)),
            IrpEntry("b", dt(2021, 1, 1), dt(2021, 6, 1)),
        )
        val merged = mergeOverlappingIrpEntries(entries)
        assertEquals(2, merged.size)
    }

    @Test
    fun `mergeOverlappingIrpEntries with single entry returns same`() {
        val entries = listOf(IrpEntry("a", dt(2021, 6, 10), dt(2022, 11, 1)))
        assertEquals(1, mergeOverlappingIrpEntries(entries).size)
    }

    @Test
    fun `mergeOverlappingIrpEntries with empty list returns empty`() {
        assertEquals(0, mergeOverlappingIrpEntries(emptyList()).size)
    }

    @Test
    fun `calculateAbsentDays with partially overlapping travels`() {
        val travels = listOf(
            Travel(dt(2020, 8, 10), dt(2020, 8, 20), "TR"),  // alone: 9 days
            Travel(dt(2020, 8, 15), dt(2020, 8, 25), "UK"),  // alone: 9 days
        )
        // Merged: Aug 10 to Aug 25 = 14 absent days
        assertEquals(14, calculateAbsentDays(travels))
    }
}
