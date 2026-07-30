import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

fun main() {
    print("Enter path to ROM file: ")
    val path = readlnOrNull()?.trim() ?: return

    val emulator = D5700Emulator()
    emulator.loadRom(path)

    val executor = Executors.newScheduledThreadPool(2)

    // CPU execution thread
    val cpuFuture = executor.scheduleAtFixedRate(
        { emulator.step() },
        0, 2, TimeUnit.MILLISECONDS
    )

    // Hardware Timer tick thread (approx 60 Hz)
    executor.scheduleAtFixedRate(
        { emulator.tickTimer() },
        0, 16, TimeUnit.MILLISECONDS
    )

    // Main control loop monitoring high-level emulator state
    while (emulator.isRunning) {
        Thread.sleep(50)
    }

    cpuFuture.cancel(true)
    executor.shutdown()
}