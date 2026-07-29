import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MemoryStrategyTest {
    @Test
    fun `write to ReadOnlyROMStrategy throws IllegalStateException`() {
        val romBytes = byteArrayOf(0x00, 0x00)
        val rom = ReadOnlyROMStrategy(romBytes)

        assertThrows<IllegalStateException> {
            rom.write(0x0000, 0x42)
        }
    }

    @Test
    fun `write to RAMStrategy succeeds`() {
        val ram = RAMStrategy()
        ram.write(0x0000, 0x42)
        assertEquals(0x42, ram.read(0x0000))
    }
}