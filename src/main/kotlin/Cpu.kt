class CPU {
    val registers = IntArray(8) { 0 } // Stores unsigned byte values (0..255)
    var pc: Int = 0x0000
    var timer: Int = 0
    var address: Int = 0x0000
    var mFlag: Int = 0
    var isRunning: Boolean = true

    fun incrementPC(step: Int = 2) {
        pc += step
    }
}