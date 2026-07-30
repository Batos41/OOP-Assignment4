// Abstract frontend output
interface DisplayDevice {
    fun renderBuffer(frameBuffer: Array<CharArray>)
}

// Console implementation of the output display
class ConsoleDisplayDevice : DisplayDevice {
    override fun renderBuffer(frameBuffer: Array<CharArray>) {
        println("\n+--------+")
        for (row in 0..7) {
            print("|")
            for (col in 0..7) {
                print(frameBuffer[row][col])
            }
            println("|")
        }
        println("+--------+")
    }
}

// Abstract input interface
interface InputDevice {
    fun readHexByte(): Int
}

// Console implementation of the keyboard input
class ConsoleInputDevice : InputDevice {
    override fun readHexByte(): Int {
        print("\nInput Hex Value (0-F): ")
        val input = readlnOrNull()?.trim() ?: ""
        if (input.isEmpty()) return 0x00
        val hexString = input.take(2)
        return hexString.toIntOrNull(16) ?: 0x00
    }
}