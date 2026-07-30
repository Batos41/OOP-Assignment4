class CPU {
    private val registers = IntArray(16)
    private var pcVal = 0
    private var timerVal = 0
    private var addressVal = 0
    private var mFlagVal = 0 // 0 = RAM, 1 = ROM
    private var running = false

    val pc: Int get() = pcVal
    val isRunning: Boolean get() = running

    fun start() { running = true }
    fun stop() { running = false }

    fun getRegister(index: Int): Int = registers[index]
    fun setRegister(index: Int, value: Int) { registers[index] = value and 0xFF }

    fun incrementPC(step: Int = 2) { pcVal += step }
    fun jumpTo(targetAddress: Int) { pcVal = targetAddress }

    fun getAddress(): Int = addressVal
    fun setAddressRegister(addr: Int) { addressVal = addr }

    val mFlag: Int get() = mFlagVal
    fun toggleMemoryFlag() { mFlagVal = if (mFlagVal == 0) 1 else 0 }
    fun resetMemoryFlag() { mFlagVal = 0 }

    fun getTimer(): Int = timerVal
    fun setTimer(value: Int) { timerVal = value }
    fun decrementTimer() { if (timerVal > 0) timerVal-- }
}