import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class InstructionTest {
    @Test
    fun `CONVERT_TO_BASE_10 writes digits to RAM at address A`() {
        val cpu = CPU().apply {
            registers[0] = 123  // Byte value to convert
            address = 0x0100    // Set address A
            mFlag = 0           // RAM mode
        }
        val bus = MemoryBus(RAMStrategy(), ReadOnlyROMStrategy(byteArrayOf()))
        val screen = Screen()
        val instruction = ConvertToBase10Instruction()

        // Instruction word for D000 (Convert r0)
        instruction.execute(cpu, bus, screen, 0xD000)

        assertEquals(1, bus.read(0x0100, cpu.mFlag)) // Hundreds digit
        assertEquals(2, bus.read(0x0101, cpu.mFlag)) // Tens digit
        assertEquals(3, bus.read(0x0102, cpu.mFlag)) // Ones digit
    }

    @Test
    fun `JUMP to odd address throws IllegalArgumentException`() {
        val cpu = CPU()
        val bus = MemoryBus(RAMStrategy(), ReadOnlyROMStrategy(byteArrayOf()))
        val screen = Screen()
        val instruction = JumpInstruction()

        // 0x5001 -> Jump to 0x0001 (odd address)
        assertThrows<IllegalArgumentException> {
            instruction.execute(cpu, bus, screen, 0x5001)
        }
    }

    @Test
    fun `DRAW with ASCII value greater than 0x7F throws IllegalArgumentException`() {
        val cpu = CPU().apply {
            registers[0] = 0x80 // Exceeds 0x7F limit
            registers[1] = 0    // Row 0
            registers[2] = 0    // Col 0
        }
        val bus = MemoryBus(RAMStrategy(), ReadOnlyROMStrategy(byteArrayOf()))
        val screen = Screen()
        val instruction = DrawInstruction()

        // 0xF012 -> Draw char in r0 at row r1, col r2
        assertThrows<IllegalArgumentException> {
            instruction.execute(cpu, bus, screen, 0xF012)
        }
    }

    @Test
    fun `CONVERT_BYTE_TO_ASCII with byte greater than 0x0F throws IllegalArgumentException`() {
        val cpu = CPU().apply {
            registers[0] = 0x10 // Exceeds 0x0F
        }
        val bus = MemoryBus(RAMStrategy(), ReadOnlyROMStrategy(byteArrayOf()))
        val screen = Screen()
        val instruction = ConvertByteToAsciiInstruction()

        // 0xE010 -> Convert r0 into r1
        assertThrows<IllegalArgumentException> {
            instruction.execute(cpu, bus, screen, 0xE010)
        }
    }

    @Test
    fun `SKIP_EQUAL advances PC by extra 2 bytes when registers are equal`() {
        val cpu = CPU().apply {
            pc = 0x0000
            registers[0] = 5
            registers[1] = 5
        }
        val bus = MemoryBus(RAMStrategy(), ReadOnlyROMStrategy(byteArrayOf()))
        val screen = Screen()
        val instruction = SkipEqualInstruction()

        // 0x8010 -> Skip if r0 == r1
        instruction.execute(cpu, bus, screen, 0x8010)

        // Expected: +2 from execute template + 2 from skip = 4 total
        assertEquals(0x0004, cpu.pc)
    }

    @Test
    fun `CONVERT_TO_BASE_10 writes to RAM even when mFlag is 1`() {
        val cpu = CPU().apply {
            registers[0] = 123
            address = 0x0100
            mFlag = 1 // ROM Mode active!
        }
        val ram = RAMStrategy()
        val rom = ReadOnlyROMStrategy(ByteArray(4096))
        val bus = MemoryBus(ram, rom)
        val screen = Screen()
        val instruction = ConvertToBase10Instruction()

        // Should NOT throw IllegalStateException from ReadOnlyROMStrategy
        assertDoesNotThrow {
            instruction.execute(cpu, bus, screen, 0xD000)
        }

        // Verify values were correctly written to RAM
        assertEquals(1, ram.read(0x0100))
        assertEquals(2, ram.read(0x0101))
        assertEquals(3, ram.read(0x0102))
    }
}