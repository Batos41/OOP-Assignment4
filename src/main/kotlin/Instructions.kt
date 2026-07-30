// Opcode 0: STORE (0, rX, bb) -> rX = bb
class StoreInstruction : Instruction() {
    override fun decode(word: Int): Operands {
        val rX = (word shr 8) and 0x0F
        val bb = word and 0xFF
        return Operands(rX = rX, byteVal = bb)
    }

    override fun applyOperation(cpu: CPU, bus: MemoryBus, screen: Screen, operands: Operands): Boolean {
        cpu.registers[operands.rX] = operands.byteVal
        return true
    }
}

// Opcode 1: ADD (1, rX, rY, rZ) -> rZ = rX + rY
class AddInstruction : Instruction() {
    override fun decode(word: Int): Operands {
        val rX = (word shr 8) and 0x0F
        val rY = (word shr 4) and 0x0F
        val rZ = word and 0x0F
        return Operands(rX = rX, rY = rY, rZ = rZ)
    }

    override fun applyOperation(cpu: CPU, bus: MemoryBus, screen: Screen, operands: Operands): Boolean {
        val sum = (cpu.registers[operands.rX] + cpu.registers[operands.rY])
        cpu.registers[operands.rZ] = sum
        return true
    }
}

// Opcode 2: SUB (2, rX, rY, rZ) -> rZ = rX - rY
class SubInstruction : Instruction() {
    override fun decode(word: Int): Operands {
        val rX = (word shr 8) and 0x0F
        val rY = (word shr 4) and 0x0F
        val rZ = word and 0x0F
        return Operands(rX = rX, rY = rY, rZ = rZ)
    }

    override fun applyOperation(cpu: CPU, bus: MemoryBus, screen: Screen, operands: Operands): Boolean {
        val diff = (cpu.registers[operands.rX] - cpu.registers[operands.rY])
        cpu.registers[operands.rZ] = diff
        return true
    }
}

// Opcode 3: READ -> Read from RAM or ROM based on cpu.mFlag
class ReadInstruction : Instruction() {
    override fun decode(word: Int): Operands = Operands(rX = (word shr 8) and 0x0F)

    override fun applyOperation(cpu: CPU, bus: MemoryBus, screen: Screen, operands: Operands): Boolean {
        // Must use cpu.mFlag (0 for RAM, 1 for ROM)
        cpu.registers[operands.rX] = bus.read(cpu.address, cpu.mFlag)
        return true
    }
}

// Opcode 4: WRITE (4, rX, 00) -> Memory[A] = rX
class WriteInstruction : Instruction() {
    override fun decode(word: Int): Operands {
        val rX = (word shr 8) and 0x0F
        return Operands(rX = rX)
    }

    override fun applyOperation(cpu: CPU, bus: MemoryBus, screen: Screen, operands: Operands): Boolean {
        bus.write(cpu.address, cpu.registers[operands.rX], cpu.mFlag)
        return true
    }
}

// Opcode 5: JUMP (5, aaa)
class JumpInstruction : Instruction() {
    override fun decode(word: Int): Operands {
        val aaa = word and 0x0FFF
        return Operands(addressVal = aaa)
    }

    override fun applyOperation(cpu: CPU, bus: MemoryBus, screen: Screen, operands: Operands): Boolean {
        if (operands.addressVal % 2 != 0) {
            throw IllegalArgumentException("Execution Error: JUMP address 0x${operands.addressVal.toString(16)} is not even.")
        }
        cpu.pc = operands.addressVal
        return false // Do NOT auto-increment PC after jump
    }
}

// Opcode 6: READ_KEYBOARD (6, rX, 00)
class ReadKeyboardInstruction : Instruction() {
    override fun decode(word: Int): Operands {
        val rX = (word shr 8) and 0x0F
        return Operands(rX = rX)
    }

    override fun applyOperation(cpu: CPU, bus: MemoryBus, screen: Screen, operands: Operands): Boolean {
        print("\nInput Hex Value (0-F): ")
        val input = readlnOrNull()?.trim() ?: ""
        if (input.isEmpty()) {
            cpu.registers[operands.rX] = 0x00
        } else {
            val hexString = input.take(2)
            val parsedVal = hexString.toIntOrNull(16) ?: 0x00
            cpu.registers[operands.rX] = parsedVal
        }
        return true
    }
}

// Opcode 7: SWITCH_MEMORY (7000) -> Toggles M flag
class SwitchMemoryInstruction : Instruction() {
    override fun decode(word: Int): Operands = Operands()

    override fun applyOperation(cpu: CPU, bus: MemoryBus, screen: Screen, operands: Operands): Boolean {
        cpu.mFlag = if (cpu.mFlag == 0) 1 else 0
        return true
    }
}

// Opcode 8: SKIP_EQUAL (8, rX, rY, 0) -> Skip next instruction if rX == rY
class SkipEqualInstruction : Instruction() {
    override fun decode(word: Int): Operands {
        val rX = (word shr 8) and 0x0F
        val rY = (word shr 4) and 0x0F
        return Operands(rX = rX, rY = rY)
    }

    override fun applyOperation(cpu: CPU, bus: MemoryBus, screen: Screen, operands: Operands): Boolean {
        if (cpu.registers[operands.rX] == cpu.registers[operands.rY]) {
            cpu.incrementPC(2) // Skip additional 2 bytes
        }
        return true // Standard +2 auto-increment will occur
    }
}

// Opcode 9: SKIP_NOT_EQUAL (9, rX, rY, 0) -> Skip next instruction if rX != rY
class SkipNotEqualInstruction : Instruction() {
    override fun decode(word: Int): Operands {
        val rX = (word shr 8) and 0x0F
        val rY = (word shr 4) and 0x0F
        return Operands(rX = rX, rY = rY)
    }

    override fun applyOperation(cpu: CPU, bus: MemoryBus, screen: Screen, operands: Operands): Boolean {
        if (cpu.registers[operands.rX] != cpu.registers[operands.rY]) {
            cpu.incrementPC(2) // Skip additional 2 bytes
        }
        return true // Standard +2 auto-increment will occur
    }
}

// Opcode A: SET_A (A, aaa) -> A = aaa
class SetAInstruction : Instruction() {
    override fun decode(word: Int): Operands {
        val aaa = word and 0x0FFF
        return Operands(addressVal = aaa)
    }

    override fun applyOperation(cpu: CPU, bus: MemoryBus, screen: Screen, operands: Operands): Boolean {
        cpu.address = operands.addressVal
        return true
    }
}

// Opcode B: SET_T (B, bb, 0) -> T = bb
class SetTInstruction : Instruction() {
    override fun decode(word: Int): Operands {
        val bb = (word shr 4 and 0xFF)
        return Operands(byteVal = bb)
    }

    override fun applyOperation(cpu: CPU, bus: MemoryBus, screen: Screen, operands: Operands): Boolean {
        cpu.timer = operands.byteVal
        return true
    }
}

// Opcode C: READ_T (C, rX, 00) -> rX = T
class ReadTInstruction : Instruction() {
    override fun decode(word: Int): Operands {
        val rX = (word shr 8) and 0x0F
        return Operands(rX = rX)
    }

    override fun applyOperation(cpu: CPU, bus: MemoryBus, screen: Screen, operands: Operands): Boolean {
        cpu.registers[operands.rX] = cpu.timer
        return true
    }
}

// Opcode D: CONVERT_TO_BASE_10 (D, rX, 00)
class ConvertToBase10Instruction : Instruction() {
    override fun decode(word: Int): Operands {
        val rX = (word shr 8) and 0x0F
        return Operands(rX = rX)
    }

    override fun applyOperation(cpu: CPU, bus: MemoryBus, screen: Screen, operands: Operands): Boolean {
        val val10 = cpu.registers[operands.rX]
        val hundreds = (val10 / 100)
        val tens = ((val10 % 100) / 10)
        val ones = (val10 % 10)

        // Standard bus writes based on current mFlag state
        bus.write(cpu.address, hundreds, cpu.mFlag)
        bus.write(cpu.address + 1, tens, cpu.mFlag)
        bus.write(cpu.address + 2, ones, cpu.mFlag)
        return true
    }
}

// Opcode E: CONVERT_BYTE_TO_ASCII (E, rX, rY, 0)
class ConvertByteToAsciiInstruction : Instruction() {
    override fun decode(word: Int): Operands {
        val rX = (word shr 8) and 0x0F
        val rY = (word shr 4) and 0x0F
        return Operands(rX = rX, rY = rY)
    }

    override fun applyOperation(cpu: CPU, bus: MemoryBus, screen: Screen, operands: Operands): Boolean {
        val rawDigit = cpu.registers[operands.rX]
        if (rawDigit > 0x0F) {
            throw IllegalArgumentException("Execution Error: Byte to convert ($rawDigit) > 0x0F")
        }
        val asciiChar = if (rawDigit < 10) '0' + rawDigit else 'a' + (rawDigit - 10)
        cpu.registers[operands.rY] = asciiChar.code
        return true
    }
}

// Opcode F: DRAW (F, rX, rY, rZ)
class DrawInstruction : Instruction() {
    override fun decode(word: Int): Operands {
        val rX = (word shr 8) and 0x0F  // Register containing ASCII char
        val rY = (word shr 4) and 0x0F  // Register containing Row (0..7)
        val rZ = word and 0x0F         // Register containing Col (0..7)
        return Operands(rX = rX, rY = rY, rZ = rZ)
    }

    override fun applyOperation(cpu: CPU, bus: MemoryBus, screen: Screen, operands: Operands): Boolean {
        val asciiVal = cpu.registers[operands.rX] and 0xFF
        // Based on what was in hello.d5700, this seems to be the intended approach.
        val row = operands.rY // Literal row number (0..7)
        val col = operands.rZ // Literal col number (0..7)

        screen.drawChar(row, col, asciiVal)
        return true
    }
}
