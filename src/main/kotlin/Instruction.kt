/**
 * Data container for parsed instruction operands.
 */
data class Operands(
    val rX: Int = 0,
    val rY: Int = 0,
    val rZ: Int = 0,
    val byteVal: Int = 0,
    val addressVal: Int = 0
)

/**
 * Abstract Base Class implementing the Template Method pattern for CPU instructions.
 */
abstract class Instruction {

    /**
     * TEMPLATE METHOD: Standard execution pipeline.
     */
    fun execute(cpu: CPU, bus: MemoryBus, screen: Screen, instructionWord: Int) {
        val operands = decode(instructionWord)
        val shouldIncrementPc = applyOperation(cpu, bus, screen, operands)
        if (shouldIncrementPc) {
            cpu.incrementPC()
        }
    }

    /**
     * Extracts operands (registers, bytes, addresses) from the 16-bit word (0x0000..0xFFFF).
     */
    protected abstract fun decode(word: Int): Operands

    /**
     * Performs the instruction operation logic.
     */
    protected abstract fun applyOperation(
        cpu: CPU,
        bus: MemoryBus,
        screen: Screen,
        operands: Operands
    ): Boolean
}