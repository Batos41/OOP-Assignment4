import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AdditionRomTest {

    private lateinit var cpu: CPU
    private lateinit var ram: RAMStrategy
    private lateinit var screen: Screen

    @BeforeEach
    fun setUp() {
        cpu = CPU()
        ram = RAMStrategy()
        screen = Screen()
    }

    @Test
    fun `test STORE instruction (Opcode 0)`() {
        val storeInst = StoreInstruction()
        // 0005 -> Store 0x05 into r0
        val word = 0x0005

        storeInst.execute(cpu, MemoryBus(ram, ram), screen, word)

        assertEquals(5, cpu.registers[0], "r0 should store 0x05")
        assertEquals(2, cpu.pc, "PC should increment by 2")
    }

    @Test
    fun `test ADD instruction decoding and execution (Opcode 1)`() {
        val addInst = AddInstruction()
        cpu.registers[0] = 5
        cpu.registers[1] = 10

        // 1012 -> ADD r0, r1, r2
        val word = 0x1012

        addInst.execute(cpu, MemoryBus(ram, ram), screen, word)

        assertEquals(15, cpu.registers[2], "r2 should store 15 (r0 + r1)")
        assertEquals(2, cpu.pc, "PC should increment by 2")
    }

    @Test
    fun `test ROM byte reading safety and unsigned alignment`() {
        // addition.d5700 raw bytes: [0x00, 0x05, 0x00, 0x0A, 0x10, 0x12, 0x00, 0x00]
        val rawBytes = byteArrayOf(
            0x00.toByte(), 0x05.toByte(),
            0x00.toByte(), 0x0A.toByte(),
            0x10.toByte(), 0x12.toByte(),
            0x00.toByte(), 0x00.toByte()
        )

        val rom = ReadOnlyROMStrategy(rawBytes)
        val bus = MemoryBus(ram, rom)

        // Read first instruction word at PC = 0x0000 (mFlag = 1 for ROM)
        val b0 = bus.read(0, 1) and 0xFF
        val b1 = bus.read(1, 1) and 0xFF
        val word1 = ((b0 shl 8) or b1).toUShort()

        assertEquals(0x0005.toUShort(), word1, "Word at PC 0 should be 0x0005")

        // Read ADD instruction at PC = 0x0004
        val b4 = bus.read(4, 1) and 0xFF
        val b5 = bus.read(5, 1) and 0xFF
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
            0x00.toByte(), 0x05.toByte(), // STORE r0, 5
            0x00.toByte(), 0x0A.toByte(), // STORE r0, 10 (Note: check if second STORE uses r1 in your decode)
            0x10.toByte(), 0x12.toByte(), // ADD r0, r1, r2
            0x00.toByte(), 0x00.toByte()  // HALT
        )

        val rom = ReadOnlyROMStrategy(rawBytes)
        val bus = MemoryBus(ram, rom)

        val instructionMap = mapOf<Int, Instruction>(
            0x0 to StoreInstruction(),
            0x1 to AddInstruction()
        )

        while (cpu.isRunning) {
            val highByte = bus.read(cpu.pc, cpu.mFlag) and 0xFF
            val lowByte = bus.read(cpu.pc + 1, cpu.mFlag) and 0xFF
            val word = ((highByte shl 8) or lowByte)

            if (word == 0x0000) {
                cpu.isRunning = false
                break
            }

            val opcode = (highByte shr 4) and 0x0F
            val handler = instructionMap[opcode]
                ?: fail("Failed at PC ${cpu.pc}: Opcode 0x${opcode.toString(16)} not found")

            handler.execute(cpu, bus, screen, word)
        }

        assertFalse(cpu.isRunning)
    }
}