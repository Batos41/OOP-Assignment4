class InstructionFactory(inputDevice: InputDevice) {
    private val registry = mapOf<Int, Instruction>(
        0x0 to StoreInstruction(),
        0x1 to AddInstruction(),
        0x2 to SubInstruction(),
        0x3 to ReadInstruction(),
        0x4 to WriteInstruction(),
        0x5 to JumpInstruction(),
        0x6 to ReadKeyboardInstruction(inputDevice),
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

    fun getInstruction(opcode: Int): Instruction {
        return registry[opcode]
            ?: throw IllegalStateException("Unknown instruction opcode: 0x${opcode.toString(16)}")
    }
}