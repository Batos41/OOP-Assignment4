import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class D5700Emulator : Runnable {
    val cpu = CPU()
    val ram = RAMStrategy()
    lateinit var memoryBus: MemoryBus
    val screen = Screen()
    
    private val instructionMap = mapOf<Int, Instruction>(
        0x0 to StoreInstruction(),
        0x1 to AddInstruction(),
        0x2 to SubInstruction(),
        0x3 to ReadInstruction(),
        0x4 to WriteInstruction(),
        0x5 to JumpInstruction(),
        0x6 to ReadKeyboardInstruction(),
        0x7 to SwitchMemoryInstruction(),
        0x8 to SkipEqualInstruction(),
        0x9 to SkipNotEqualInstruction(),
        0x0A to SetAInstruction(),
        0x0B to SetTInstruction(),
        0x0C to ReadTInstruction(),
        0x0D to ConvertToBase10Instruction(),
        0x0E to ConvertByteToAsciiInstruction(),
        0x0F to DrawInstruction()
    )

    fun loadRom(filePath: String) {
        val bytes = File(filePath).readBytes()
        val rom = ReadOnlyROMStrategy(bytes)
        memoryBus = MemoryBus(ram, rom)

        // Ensure the CPU starts at address 0 in ROM memory
        cpu.isRunning = true
    }

    override fun run() {
        if (!cpu.isRunning) return

        try {
            // High and low bytes are already clean Ints in range 0..255
            val highByte = memoryBus.rom.read(cpu.pc)
            val lowByte = memoryBus.rom.read(cpu.pc + 1)
            val word = (highByte shl 8) or lowByte

            // DEBUG PRINT:
            //println("FETCH: PC=0x${cpu.pc.toString(16).padStart(4, '0')} | M=${cpu.mFlag} | High=0x${highByte.toString(16)} Low=0x${lowByte.toString(16)} | Word=0x${word.toString(16).padStart(4, '0')}")

            if (word == 0x0000) {
                println("Instruction 0000 reached. Halting CPU.")
                screen.render() // Render final screen frame on halt
                cpu.isRunning = false
                return
            }

            val opcode = (highByte shr 4) and 0x0F
            val handler = instructionMap[opcode]
                ?: throw IllegalStateException("Unknown instruction opcode: 0x${opcode.toString(16)}")

            handler.execute(cpu, memoryBus, screen, word)
        } catch (e: Exception) {
            println("CPU Execution Error: ${e.message}")
            cpu.isRunning = false
        }
    }
}

fun main() {
    print("Enter path to ROM file: ")
    val path = readlnOrNull()?.trim() ?: return

    val emulator = D5700Emulator()
    emulator.loadRom(path)

    val executor = Executors.newScheduledThreadPool(2)

    // CPU execution scheduled at 500Hz (every 2 ms)
    val cpuFuture = executor.scheduleAtFixedRate(
        emulator,
        0,
        2,
        TimeUnit.MILLISECONDS
    )

    // Timer decrements at 60Hz (every 16 ms) even during pause/keyboard prompt
    executor.scheduleAtFixedRate({
        if (emulator.cpu.timer > 0x00) {
            emulator.cpu.timer--
        }
    }, 0, 16, TimeUnit.MILLISECONDS)

    // Wait loop until CPU halts
    while (emulator.cpu.isRunning) {
        Thread.sleep(50)
    }

    cpuFuture.cancel(true)
    executor.shutdown()
}