import java.time.LocalDateTime
import java.util.Date
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ParseDateTest {

    @Test
    fun `parses valid date string`() {
        val result = parseDate("2020-01-15")
        assertEquals(LocalDateTime.of(2020, 1, 15, 0, 0), result)
    }

    @Test
    fun `parses date string with whitespace`() {
        val result = parseDate("  2020-01-15  ")
        assertEquals(LocalDateTime.of(2020, 1, 15, 0, 0), result)
    }

    @Test
    fun `parses java util Date`() {
        val cal = java.util.Calendar.getInstance(TimeZone.getTimeZone("GMT"))
        cal.set(2020, 0, 15, 0, 0, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val result = parseDate(cal.time)
        assertEquals(LocalDateTime.of(2020, 1, 15, 0, 0), result)
    }

    @Test
    fun `rejects invalid date format dd-MM-yyyy`() {
        assertFailsWith<IllegalArgumentException>("Invalid date format") {
            parseDate("15-01-2020")
        }
    }

    @Test
    fun `rejects date with extra text`() {
        assertFailsWith<IllegalArgumentException>("Invalid date format") {
            parseDate("2020-01-15T00:00:00")
        }
    }

    @Test
    fun `rejects non-date string`() {
        assertFailsWith<IllegalArgumentException>("Invalid date format") {
            parseDate("not-a-date")
        }
    }

    @Test
    fun `rejects empty string`() {
        assertFailsWith<IllegalArgumentException>("Invalid date format") {
            parseDate("")
        }
    }
}
