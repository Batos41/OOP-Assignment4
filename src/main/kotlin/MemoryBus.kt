class MemoryBus(
    private val ram: MemoryStrategy,
    private var rom: MemoryStrategy
) {
    fun updateRom(newRom: MemoryStrategy) {
        this.rom = newRom
    }

    fun read(address: Int, mFlag: Int): Int {
        return if (mFlag == 1) rom.read(address) else ram.read(address)
    }

    fun write(address: Int, value: Int, mFlag: Int) {
        if (mFlag == 1) {
            throw IllegalStateException("Execution Error: Cannot write to ROM at address 0x${address.toString(16)}")
        }
        ram.write(address, value)
    }

    // Encapsulates 16-bit instruction word fetching directly from ROM
    fun fetchInstructionWord(pc: Int): Int {
        val highByte = rom.read(pc) and 0xFF
        val lowByte = rom.read(pc + 1) and 0xFF
        return (highByte shl 8) or lowByte
    }
}