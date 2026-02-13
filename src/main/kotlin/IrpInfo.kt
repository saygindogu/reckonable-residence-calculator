import java.time.LocalDateTime

data class IrpEntry(val name: String, val start: LocalDateTime, val end: LocalDateTime)

data class IrpInfo(val irelandFirstEntry: LocalDateTime, val irpEntries: List<IrpEntry>)
