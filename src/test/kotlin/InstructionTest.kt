import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class InstructionTest {
    private lateinit var cpu: CPU
    private lateinit var ram: RAMStrategy
    private lateinit var rom: ReadOnlyROMStrategy
    private lateinit var bus: MemoryBus
    private lateinit var screen: Screen
    private lateinit var testDisplay: DisplayDevice

    // Test double for InputDevice to mock keyboard inputs cleanly
    private class TestInputDevice(var nextValue: Int = 0x00) : InputDevice {
        override fun readHexByte(): Int = nextValue
    }

    @BeforeEach
    fun setUp() {
        cpu = CPU()
        ram = RAMStrategy()
        rom = ReadOnlyROMStrategy(ByteArray(4096))
        bus = MemoryBus(ram, rom)

        // Stub DisplayDevice to prevent output during testing
        testDisplay = object : DisplayDevice {
            override fun renderBuffer(frameBuffer: Array<CharArray>) {}
        }
        screen = Screen(testDisplay)
    }

    @Test
    fun `CONVERT_TO_BASE_10 writes digits to RAM at address A`() {
        cpu.setRegister(0, 123)       // Byte value to convert
        cpu.setAddressRegister(0x0100) // Set address A
        cpu.resetMemoryFlag()         // RAM mode

        val testBus = MemoryBus(RAMStrategy(), ReadOnlyROMStrategy(byteArrayOf()))
        val instruction = ConvertToBase10Instruction()

        // Instruction word for D000 (Convert r0)
        instruction.execute(cpu, testBus, screen, 0xD000)

        assertEquals(1, testBus.read(0x0100, cpu.mFlag)) // Hundreds digit
        assertEquals(2, testBus.read(0x0101, cpu.mFlag)) // Tens digit
        assertEquals(3, testBus.read(0x0102, cpu.mFlag)) // Ones digit
    }

    @Test
    fun `JUMP to odd address throws IllegalArgumentException`() {
        cpu.start()
        val testBus = MemoryBus(RAMStrategy(), ReadOnlyROMStrategy(byteArrayOf()))
        val instruction = JumpInstruction()

        // 0x5001 -> Jump to 0x0001 (odd address)
        assertThrows<IllegalArgumentException> {
            instruction.execute(cpu, testBus, screen, 0x5001)
        }
    }

    @Test
    fun `DRAW with ASCII value greater than 0x7F throws IllegalArgumentException`() {
        cpu.setRegister(0, 0x80) // Exceeds 0x7F limit
        cpu.setRegister(1, 0)    // Row 0
        cpu.setRegister(2, 0)    // Col 0

        val testBus = MemoryBus(RAMStrategy(), ReadOnlyROMStrategy(byteArrayOf()))
        val instruction = DrawInstruction()

        // 0xF012 -> Draw char in r0 at row r1, col r2
        assertThrows<IllegalArgumentException> {
            instruction.execute(cpu, testBus, screen, 0xF012)
        }
    }

    @Test
    fun `CONVERT_BYTE_TO_ASCII with byte greater than 0x0F throws IllegalArgumentException`() {
        cpu.setRegister(0, 0x10) // Exceeds 0x0F

        val testBus = MemoryBus(RAMStrategy(), ReadOnlyROMStrategy(byteArrayOf()))
        val instruction = ConvertByteToAsciiInstruction()

        // 0xE010 -> Convert r0 into r1
        assertThrows<IllegalArgumentException> {
            instruction.execute(cpu, testBus, screen, 0xE010)
        }
    }

    @Test
    fun `SKIP_EQUAL advances PC by extra 2 bytes when registers are equal`() {
        cpu.setRegister(0, 5)
        cpu.setRegister(1, 5)

        val testBus = MemoryBus(RAMStrategy(), ReadOnlyROMStrategy(byteArrayOf()))
        val instruction = SkipEqualInstruction()

        // 0x8010 -> Skip if r0 == r1
        instruction.execute(cpu, testBus, screen, 0x8010)

        // Expected: +2 from execute template + 2 from skip = 4 total
        assertEquals(0x0004, cpu.pc)
    }

    // --- Opcode 4: WRITE ---

    @Test
    fun `WRITE stores register value at CPU address in RAM when mFlag is 0`() {
        cpu.setRegister(2, 0xAB)
        cpu.setAddressRegister(0x0100)
        cpu.resetMemoryFlag()

        val instruction = WriteInstruction()
        // Word 0x4200 -> WRITE r2
        instruction.execute(cpu, bus, screen, 0x4200)

        assertEquals(0xAB, ram.read(0x0100))
    }

    @Test
    fun `WRITE throws IllegalStateException when mFlag is 1 (ROM mode)`() {
        cpu.setRegister(2, 0xAB)
        cpu.setAddressRegister(0x0100)
        cpu.toggleMemoryFlag() // Set mFlag to 1 (ROM mode)

        val instruction = WriteInstruction()

        assertThrows(IllegalStateException::class.java) {
            instruction.execute(cpu, bus, screen, 0x4200)
        }
    }

    // --- Opcode 5: JUMP ---

    @Test
    fun `JUMP sets program counter to target even address without auto-incrementing`() {
        cpu.jumpTo(0x0002)
        val instruction = JumpInstruction()

        // Word 0x51A2 -> JUMP 0x01A2
        instruction.execute(cpu, bus, screen, 0x51A2)

        assertEquals(0x01A2, cpu.pc)
    }

    @Test
    fun `JUMP throws IllegalArgumentException for odd address`() {
        val instruction = JumpInstruction()

        // Word 0x51A3 -> Odd address 0x01A3
        assertThrows(IllegalArgumentException::class.java) {
            instruction.execute(cpu, bus, screen, 0x51A3)
        }
    }

    // --- Opcode 6: READ_KEYBOARD ---

    @Test
    fun `READ_KEYBOARD parses input hex digit into target register`() {
        val inputDouble = TestInputDevice(nextValue = 0x0A)
        val instruction = ReadKeyboardInstruction(inputDouble)

        // Word 0x6300 -> READ_KEYBOARD r3
        instruction.execute(cpu, bus, screen, 0x6300)

        assertEquals(0x0A, cpu.getRegister(3))
    }

    @Test
    fun `READ_KEYBOARD sets register to 0x00 on empty input`() {
        val inputDouble = TestInputDevice(nextValue = 0x00)
        val instruction = ReadKeyboardInstruction(inputDouble)

        instruction.execute(cpu, bus, screen, 0x6300)

        assertEquals(0x00, cpu.getRegister(3))
    }

    // --- Opcode 7: SWITCH_MEMORY ---

    @Test
    fun `SWITCH_MEMORY toggles mFlag from 0 to 1 and 1 to 0`() {
        val instruction = SwitchMemoryInstruction()

        cpu.resetMemoryFlag()
        instruction.execute(cpu, bus, screen, 0x7000)
        assertEquals(1, cpu.mFlag)

        instruction.execute(cpu, bus, screen, 0x7000)
        assertEquals(0, cpu.mFlag)
    }

    // --- Opcode 8: SKIP_EQUAL ---

    @Test
    fun `SKIP_EQUAL skips extra 2 bytes when registers are equal`() {
        cpu.setRegister(1, 0x42)
        cpu.setRegister(2, 0x42)
        cpu.jumpTo(0x0010)

        val instruction = SkipEqualInstruction()
        // Word 0x8120 -> Compare r1 and r2
        instruction.execute(cpu, bus, screen, 0x8120)

        // Skipped 2 bytes (+2 inside applyOperation) + standard 2 bytes (+2 inside execute) = +4
        assertEquals(0x0014, cpu.pc)
    }

    @Test
    fun `SKIP_EQUAL advances PC normally when registers are not equal`() {
        cpu.setRegister(1, 0x42)
        cpu.setRegister(2, 0x99)
        cpu.jumpTo(0x0010)

        val instruction = SkipEqualInstruction()
        instruction.execute(cpu, bus, screen, 0x8120)

        assertEquals(0x0012, cpu.pc)
    }

    // --- Opcode 9: SKIP_NOT_EQUAL ---

    @Test
    fun `SKIP_NOT_EQUAL skips extra 2 bytes when registers are not equal`() {
        cpu.setRegister(1, 0x42)
        cpu.setRegister(2, 0x99)
        cpu.jumpTo(0x0010)

        val instruction = SkipNotEqualInstruction()
        // Word 0x9120 -> Compare r1 and r2
        instruction.execute(cpu, bus, screen, 0x9120)

        assertEquals(0x0014, cpu.pc)
    }

    @Test
    fun `SKIP_NOT_EQUAL advances PC normally when registers are equal`() {
        cpu.setRegister(1, 0x42)
        cpu.setRegister(2, 0x42)
        cpu.jumpTo(0x0010)

        val instruction = SkipNotEqualInstruction()
        instruction.execute(cpu, bus, screen, 0x9120)

        assertEquals(0x0012, cpu.pc)
    }

    // --- Opcode B: SET_T ---

    @Test
    fun `SET_T updates CPU timer register`() {
        val instruction = SetTInstruction()
        // Word 0xB3C0 -> SET_T 0x3C (60)
        instruction.execute(cpu, bus, screen, 0xB3C0)

        assertEquals(0x3C, cpu.getTimer())
    }

    // --- Opcode C: READ_T ---

    @Test
    fun `READ_T loads current timer value into target register`() {
        cpu.setTimer(0x1F)
        val instruction = ReadTInstruction()

        // Word 0xC400 -> READ_T r4
        instruction.execute(cpu, bus, screen, 0xC400)

        assertEquals(0x1F, cpu.getRegister(4))
    }

    // --- Opcode 2: SUB ---

    @Test
    fun `SUB subtracts rY from rX and stores result in rZ`() {
        cpu.setRegister(1, 0x0A) // 10
        cpu.setRegister(2, 0x04) // 4

        val instruction = SubInstruction()
        // Word 0x2123 -> SUB r1 (rX), r2 (rY), r3 (rZ)
        instruction.execute(cpu, bus, screen, 0x2123)

        assertEquals(6, cpu.getRegister(3))
    }

    @Test
    fun `SUB handles underflow correctly`() {
        cpu.setRegister(1, 0x02)
        cpu.setRegister(2, 0x05)

        val instruction = SubInstruction()
        // Word 0x2123 -> SUB r1, r2, r3 (2 - 5 = -3)
        instruction.execute(cpu, bus, screen, 0x2123)

        // Stored value should mask into 8-bit unsigned integer representation
        assertEquals(0xFD, cpu.getRegister(3))
    }

    // --- Opcode 3: READ ---

    @Test
    fun `READ loads value from RAM when cpu mFlag is 0`() {
        cpu.setAddressRegister(0x0050)
        cpu.resetMemoryFlag()
        ram.write(0x0050, 0x7F)

        val instruction = ReadInstruction()
        // Word 0x3400 -> READ r4
        instruction.execute(cpu, bus, screen, 0x3400)

        assertEquals(0x7F, cpu.getRegister(4))
    }

    @Test
    fun `READ loads value from ROM when cpu mFlag is 1`() {
        val customRomBytes = ByteArray(4096)
        customRomBytes[0x0050] = 0x3B.toByte()

        val customRom = ReadOnlyROMStrategy(customRomBytes)
        val customBus = MemoryBus(ram, customRom)

        cpu.setAddressRegister(0x0050)
        cpu.toggleMemoryFlag() // Set mFlag to 1 (ROM mode)

        val instruction = ReadInstruction()
        // Word 0x3400 -> READ r4
        instruction.execute(cpu, customBus, screen, 0x3400)

        assertEquals(0x3B, cpu.getRegister(4))
    }

    // --- Opcode A: SET_A ---

    @Test
    fun `SET_A sets CPU address register to specified 12-bit address`() {
        val instruction = SetAInstruction()

        // Word 0xA123 -> SET_A 0x123
        instruction.execute(cpu, bus, screen, 0xA123)

        assertEquals(0x0123, cpu.getAddress())
    }

    @Test
    fun `SET_A correctly parses max 12-bit address value`() {
        val instruction = SetAInstruction()

        // Word 0xAFFF -> SET_A 0xFFF
        instruction.execute(cpu, bus, screen, 0xAFFF)

        assertEquals(0x0FFF, cpu.getAddress())
    }

    // --- Opcode E: CONVERT_BYTE_TO_ASCII ---

    @Test
    fun `CONVERT_BYTE_TO_ASCII converts single digit hex 0-9 to ASCII character byte`() {
        cpu.setRegister(0, 0x07) // Value 7

        val instruction = ConvertByteToAsciiInstruction()
        // Word 0xE010 -> Convert byte in r0, store ASCII in r1
        instruction.execute(cpu, bus, screen, 0xE010)

        assertEquals('7'.code, cpu.getRegister(1)) // ASCII '7' (0x37 / 55)
    }

    @Test
    fun `CONVERT_BYTE_TO_ASCII converts hex A-F to ASCII lowercase character byte`() {
        cpu.setRegister(0, 0x0E) // Value 14 (hex 'e')

        val instruction = ConvertByteToAsciiInstruction()
        // Word 0xE010 -> Convert byte in r0, store ASCII in r1
        instruction.execute(cpu, bus, screen, 0xE010)

        assertEquals('e'.code, cpu.getRegister(1)) // ASCII 'e' (0x65 / 101)
    }

    @Test
    fun `CONVERT_BYTE_TO_ASCII throws IllegalArgumentException if input value exceeds 0x0F`() {
        cpu.setRegister(0, 0x10) // Value 16 (> 15 / 0x0F)

        val instruction = ConvertByteToAsciiInstruction()

        assertThrows(IllegalArgumentException::class.java) {
            instruction.execute(cpu, bus, screen, 0xE010)
        }
    }
}