// Updated ReadKeyboardInstruction using dependency injection
class ReadKeyboardInstruction(private val inputDevice: InputDevice) : Instruction() {
    override fun decode(word: Int): Operands = Operands(rX = (word shr 8) and 0x0F)

    override fun applyOperation(cpu: CPU, bus: MemoryBus, screen: Screen, operands: Operands): Boolean {
        val value = inputDevice.readHexByte()
        cpu.setRegister(operands.rX, value)
        return true
    }
}

// Opcode 0: STORE
class StoreInstruction : Instruction() {
    override fun decode(word: Int): Operands = Operands(rX = (word shr 8) and 0x0F, byteVal = word and 0xFF)

    override fun applyOperation(cpu: CPU, bus: MemoryBus, screen: Screen, operands: Operands): Boolean {
        cpu.setRegister(operands.rX, operands.byteVal)
        return true
    }
}

// Opcode 1: ADD
class AddInstruction : Instruction() {
    override fun decode(word: Int): Operands = Operands(rX = (word shr 8) and 0x0F, rY = (word shr 4) and 0x0F, rZ = word and 0x0F)

    override fun applyOperation(cpu: CPU, bus: MemoryBus, screen: Screen, operands: Operands): Boolean {
        val sum = cpu.getRegister(operands.rX) + cpu.getRegister(operands.rY)
        cpu.setRegister(operands.rZ, sum)
        return true
    }
}

// Opcode 2: SUB
class SubInstruction : Instruction() {
    override fun decode(word: Int): Operands = Operands(rX = (word shr 8) and 0x0F, rY = (word shr 4) and 0x0F, rZ = word and 0x0F)

    override fun applyOperation(cpu: CPU, bus: MemoryBus, screen: Screen, operands: Operands): Boolean {
        val diff = cpu.getRegister(operands.rX) - cpu.getRegister(operands.rY)
        cpu.setRegister(operands.rZ, diff)
        return true
    }
}

// Opcode 3: READ
class ReadInstruction : Instruction() {
    override fun decode(word: Int): Operands = Operands(rX = (word shr 8) and 0x0F)

    override fun applyOperation(cpu: CPU, bus: MemoryBus, screen: Screen, operands: Operands): Boolean {
        val valFromMem = bus.read(cpu.getAddress(), cpu.mFlag)
        cpu.setRegister(operands.rX, valFromMem)
        return true
    }
}

// Opcode 4: WRITE
class WriteInstruction : Instruction() {
    override fun decode(word: Int): Operands = Operands(rX = (word shr 8) and 0x0F)

    override fun applyOperation(cpu: CPU, bus: MemoryBus, screen: Screen, operands: Operands): Boolean {
        bus.write(cpu.getAddress(), cpu.getRegister(operands.rX), cpu.mFlag)
        return true
    }
}

// Opcode 5: JUMP
class JumpInstruction : Instruction() {

    override fun decode(word: Int): Operands {
        // Extracts lower 12 bits as target address
        val addressVal = word and 0x0FFF
        return Operands(rX = 0, rY = 0, rZ = 0, byteVal = 0, addressVal = addressVal)
    }

    override fun applyOperation(
        cpu: CPU,
        bus: MemoryBus,
        screen: Screen,
        operands: Operands
    ): Boolean {
        // Enforce 2-byte alignment rule
        if (operands.addressVal % 2 != 0) {
            throw IllegalArgumentException(
                "Execution Error: JUMP target address must be even (got 0x${operands.addressVal.toString(16)})"
            )
        }

        cpu.jumpTo(operands.addressVal)

        // Return false so the emulator loop knows NOT to perform auto pc += 2
        return false
    }
}

// Opcode 7: SWITCH_MEMORY
class SwitchMemoryInstruction : Instruction() {
    override fun decode(word: Int): Operands = Operands()

    override fun applyOperation(cpu: CPU, bus: MemoryBus, screen: Screen, operands: Operands): Boolean {
        cpu.toggleMemoryFlag()
        return true
    }
}

// Opcode 8: SKIP_EQUAL
class SkipEqualInstruction : Instruction() {
    override fun decode(word: Int): Operands = Operands(rX = (word shr 8) and 0x0F, rY = (word shr 4) and 0x0F)

    override fun applyOperation(cpu: CPU, bus: MemoryBus, screen: Screen, operands: Operands): Boolean {
        if (cpu.getRegister(operands.rX) == cpu.getRegister(operands.rY)) {
            cpu.incrementPC(2)
        }
        return true
    }
}

// Opcode 9: SKIP_NOT_EQUAL
class SkipNotEqualInstruction : Instruction() {
    override fun decode(word: Int): Operands = Operands(rX = (word shr 8) and 0x0F, rY = (word shr 4) and 0x0F)

    override fun applyOperation(cpu: CPU, bus: MemoryBus, screen: Screen, operands: Operands): Boolean {
        if (cpu.getRegister(operands.rX) != cpu.getRegister(operands.rY)) {
            cpu.incrementPC(2)
        }
        return true
    }
}

// Opcode A: SET_A
class SetAInstruction : Instruction() {
    override fun decode(word: Int): Operands = Operands(addressVal = word and 0x0FFF)

    override fun applyOperation(cpu: CPU, bus: MemoryBus, screen: Screen, operands: Operands): Boolean {
        cpu.setAddressRegister(operands.addressVal)
        return true
    }
}

// Opcode B: SET_T
class SetTInstruction : Instruction() {
    override fun decode(word: Int): Operands = Operands(byteVal = (word shr 4) and 0xFF)

    override fun applyOperation(cpu: CPU, bus: MemoryBus, screen: Screen, operands: Operands): Boolean {
        cpu.setTimer(operands.byteVal)
        return true
    }
}

// Opcode C: READ_T
class ReadTInstruction : Instruction() {
    override fun decode(word: Int): Operands = Operands(rX = (word shr 8) and 0x0F)

    override fun applyOperation(cpu: CPU, bus: MemoryBus, screen: Screen, operands: Operands): Boolean {
        cpu.setRegister(operands.rX, cpu.getTimer())
        return true
    }
}

// Opcode D: CONVERT_TO_BASE_10
class ConvertToBase10Instruction : Instruction() {
    override fun decode(word: Int): Operands = Operands(rX = (word shr 8) and 0x0F)

    override fun applyOperation(cpu: CPU, bus: MemoryBus, screen: Screen, operands: Operands): Boolean {
        val val10 = cpu.getRegister(operands.rX)
        val hundreds = (val10 / 100)
        val tens = ((val10 % 100) / 10)
        val ones = (val10 % 10)

        bus.write(cpu.getAddress(), hundreds, cpu.mFlag)
        bus.write(cpu.getAddress() + 1, tens, cpu.mFlag)
        bus.write(cpu.getAddress() + 2, ones, cpu.mFlag)
        return true
    }
}

// Opcode E: CONVERT_BYTE_TO_ASCII
class ConvertByteToAsciiInstruction : Instruction() {
    override fun decode(word: Int): Operands = Operands(rX = (word shr 8) and 0x0F, rY = (word shr 4) and 0x0F)

    override fun applyOperation(cpu: CPU, bus: MemoryBus, screen: Screen, operands: Operands): Boolean {
        val rawDigit = cpu.getRegister(operands.rX)
        if (rawDigit > 0x0F) {
            throw IllegalArgumentException("Execution Error: Byte to convert ($rawDigit) > 0x0F")
        }
        val asciiChar = if (rawDigit < 10) '0' + rawDigit else 'a' + (rawDigit - 10)
        cpu.setRegister(operands.rY, asciiChar.code)
        return true
    }
}

// Opcode F: DRAW
class DrawInstruction : Instruction() {
    override fun decode(word: Int): Operands = Operands(rX = (word shr 8) and 0x0F, rY = (word shr 4) and 0x0F, rZ = word and 0x0F)

    override fun applyOperation(cpu: CPU, bus: MemoryBus, screen: Screen, operands: Operands): Boolean {
        val asciiVal = cpu.getRegister(operands.rX)
        screen.drawChar(operands.rY, operands.rZ, asciiVal)
        return true
    }
}