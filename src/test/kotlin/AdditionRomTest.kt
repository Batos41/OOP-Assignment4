import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AdditionRomTest {

    private lateinit var cpu: CPU
    private lateinit var ram: RAMStrategy
    private lateinit var screen: Screen
    private lateinit var testDisplay: DisplayDevice
    private lateinit var testInput: InputDevice
    private lateinit var factory: InstructionFactory

    @BeforeEach
    fun setUp() {
        cpu = CPU()
        ram = RAMStrategy()

        // Stub DisplayDevice to prevent standard output during unit tests
        testDisplay = object : DisplayDevice {
            override fun renderBuffer(frameBuffer: Array<CharArray>) {}
        }

        // Stub InputDevice returning fixed hex byte 0x00 for automated testing
        testInput = object : InputDevice {
            override fun readHexByte(): Int = 0x00
        }

        screen = Screen(testDisplay)
        factory = InstructionFactory(testInput)
    }

    @Test
    fun `test STORE instruction (Opcode 0)`() {
        val storeInst = factory.getInstruction(0x0)
        // 0005 -> Store 0x05 into r0
        val word = 0x0005

        storeInst.execute(cpu, MemoryBus(ram, ram), screen, word)

        assertEquals(5, cpu.getRegister(0), "r0 should store 0x05")
        assertEquals(2, cpu.pc, "PC should increment by 2")
    }

    @Test
    fun `test ADD instruction decoding and execution (Opcode 1)`() {
        val addInst = factory.getInstruction(0x1)
        cpu.setRegister(0, 5)
        cpu.setRegister(1, 10)

        // 1012 -> ADD r0, r1, r2
        val word = 0x1012

        addInst.execute(cpu, MemoryBus(ram, ram), screen, word)

        assertEquals(15, cpu.getRegister(2), "r2 should store 15 (r0 + r1)")
        assertEquals(2, cpu.pc, "PC should increment by 2")
    }

    @Test
    fun `test ROM byte reading safety and unsigned alignment`() {
        // addition test program raw bytes
        val rawBytes = byteArrayOf(
            0x00.toByte(), 0x05.toByte(), // STORE r0, 5
            0x01.toByte(), 0x0A.toByte(), // STORE r1, 10
            0x10.toByte(), 0x12.toByte(), // ADD r0, r1, r2
            0x00.toByte(), 0x00.toByte()  // HALT
        )

        val rom = ReadOnlyROMStrategy(rawBytes)

        // Read first instruction word at PC = 0x0000 directly from ROM
        val b0 = rom.read(0)
        val b1 = rom.read(1)
        val word1 = ((b0 shl 8) or b1).toUShort()

        assertEquals(0x0005.toUShort(), word1, "Word at PC 0 should be 0x0005")

        // Read ADD instruction at PC = 0x0004
        val b4 = rom.read(4)
        val b5 = rom.read(5)
        val wordADD = ((b4 shl 8) or b5).toUShort()

        assertEquals(0x1012.toUShort(), wordADD, "Word at PC 4 should be 0x1012")

        val opcode = (b4 shr 4) and 0x0F
        val rX = b4 and 0x0F
        val rY = (b5 shr 4) and 0x0F
        val rZ = b5 and 0x0F

        assertEquals(1, opcode, "Opcode should be 1")
        assertEquals(0, rX, "rX should be 0")
        assertEquals(1, rY, "rY should be 1")
        assertEquals(2, rZ, "rZ should be 2")
    }

    @Test
    fun `test full addition execution loop`() {
        val rawBytes = byteArrayOf(
            0x00.toByte(), 0x05.toByte(), // STORE r0, 5 (0x0005)
            0x01.toByte(), 0x0A.toByte(), // STORE r1, 10 (0x010A)
            0x10.toByte(), 0x12.toByte(), // ADD r0, r1, r2 (0x1012)
            0x00.toByte(), 0x00.toByte()  // HALT (0x0000)
        )

        val rom = ReadOnlyROMStrategy(rawBytes)
        val bus = MemoryBus(ram, rom)

        cpu.start()

        while (cpu.isRunning) {
            // High-level fetch hides raw ROM byte reading and bit-shifting
            val word = bus.fetchInstructionWord(cpu.pc)

            if (word == 0x0000) {
                cpu.stop()
                break
            }

            // Opcode extraction from the 16-bit word
            val opcode = (word shr 12) and 0x0F
            val handler = factory.getInstruction(opcode)

            handler.execute(cpu, bus, screen, word)
        }

        assertFalse(cpu.isRunning)
        assertEquals(5, cpu.getRegister(0))
        assertEquals(10, cpu.getRegister(1))
        assertEquals(15, cpu.getRegister(2))
    }
}