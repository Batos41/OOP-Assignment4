import java.io.File

class D5700Emulator(
    displayDevice: DisplayDevice = ConsoleDisplayDevice(),
    inputDevice: InputDevice = ConsoleInputDevice()
) : Runnable {

    private val cpu = CPU()
    private val ram = RAMStrategy()
    private val screen = Screen(displayDevice)
    private val instructionFactory = InstructionFactory(inputDevice)
    private val memoryBus = MemoryBus(ram, ReadOnlyROMStrategy(byteArrayOf()))

    val isRunning: Boolean
        get() = cpu.isRunning

    fun loadRom(filePath: String) {
        val bytes = File(filePath).readBytes()
        memoryBus.updateRom(ReadOnlyROMStrategy(bytes))
        cpu.start()
    }

    fun step() {
        if (!cpu.isRunning) return

        try {
            val word = memoryBus.fetchInstructionWord(cpu.pc)

            if (word == 0x0000) {
                screen.render()
                cpu.stop()
                return
            }

            val opcode = (word shr 12) and 0x0F
            val handler = instructionFactory.getInstruction(opcode)

            handler.execute(cpu, memoryBus, screen, word)
        } catch (e: Exception) {
            println("CPU Execution Error: ${e.message}")
            cpu.stop()
        }
    }

    override fun run() {
        step()
    }

    fun tickTimer() {
        if (cpu.isRunning) {
            cpu.decrementTimer()
        }
    }

    fun renderScreen() {
        screen.render()
    }
}