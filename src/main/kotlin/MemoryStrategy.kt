interface MemoryStrategy {
    /** Reads an unsigned byte (returned as an Int in range 0..255) at address */
    fun read(address: Int): Int

    /** Writes an unsigned byte value (0..255) to address */
    fun write(address: Int, value: Int)
}

class RAMStrategy(size: Int = 4096) : MemoryStrategy {
    private val data = IntArray(size) { 0 }

    override fun read(address: Int): Int {
        val safeAddr = (address and 0xFFFF) % data.size
        return data[safeAddr] and 0xFF
    }

    override fun write(address: Int, value: Int) {
        val safeAddr = (address and 0xFFFF) % data.size
        data[safeAddr] = value and 0xFF
    }
}

class ReadOnlyROMStrategy(romBytes: ByteArray) : MemoryStrategy {
    private val data = IntArray(4096) { 0 }

    init {
        // Copy ROM content into 4KB space, masking signed bytes into 0..255 range
        for (i in romBytes.indices) {
            if (i < data.size) {
                data[i] = romBytes[i].toInt() and 0xFF
            }
        }
    }

    override fun read(address: Int): Int {
        val safeAddr = (address and 0xFFFF) % data.size
        return data[safeAddr] and 0xFF
    }

    override fun write(address: Int, value: Int) {
        throw IllegalStateException(
            "Execution Error: Attempted write operation to read-only ROM at address 0x${address.toString(16)}"
        )
    }
}

class MemoryBus(val ram: MemoryStrategy, var rom: MemoryStrategy) {
    fun read(address: Int, mFlag: Int): Int {
        return if (mFlag == 1) rom.read(address) else ram.read(address)
    }

    fun write(address: Int, value: Int, mFlag: Int) {
        if (mFlag == 1) {
            rom.write(address, value)
        } else {
            ram.write(address, value)
        }
    }
}