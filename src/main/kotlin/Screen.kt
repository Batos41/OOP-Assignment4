class Screen(private val displayDevice: DisplayDevice) {
    private val frameBuffer = Array(8) { CharArray(8) { ' ' } }

    fun drawChar(row: Int, col: Int, asciiByte: Int) {
        if (asciiByte > 0x7F) {
            throw IllegalArgumentException("Execution Error: ASCII value > 0x7F ($asciiByte)")
        }
        if (row in 0..7 && col in 0..7) {
            frameBuffer[row][col] = asciiByte.toChar()
        }
    }

    // High-level component decides when to trigger this
    fun render() {
        displayDevice.renderBuffer(frameBuffer)
    }
}