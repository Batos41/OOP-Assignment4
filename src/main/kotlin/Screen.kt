class Screen {
    private val frameBuffer = Array(8) { CharArray(8) { ' ' } }

    fun drawChar(row: Int, col: Int, asciiByte: Int) {
        if (asciiByte > 0x7F) {
            throw IllegalArgumentException("Execution Error: ASCII character value > 0x7F ($asciiByte)")
        }
        if (row in 0..7 && col in 0..7) {
            frameBuffer[row][col] = asciiByte.toChar()
            render() // <--- Print update to console immediately
        }
    }

    fun render() {
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