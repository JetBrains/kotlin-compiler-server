import kotlin.concurrent.atomics.*
import kotlin.random.Random

@OptIn(ExperimentalAtomicApi::class)
fun main() {
    val counter = AtomicLong(Random.nextLong())
    val minSetBitsThreshold = 20

    // Sets a new value without using the result
    counter.update { if (it < 0xDECAF) 0xCACA0 else 0xC0FFEE }

    // Retrieves the current value, then updates it
    val previousValue = counter.fetchAndUpdate { 0x1CEDL.shl(Long.SIZE_BITS - it.countLeadingZeroBits()) or it }

    // Updates the value, then retrieves the result
    val current = counter.updateAndFetch {
        if (it.countOneBits() < minSetBitsThreshold) it.shl(20) or 0x15BADL else it
    }

    val hexFormat = HexFormat {
        upperCase = true
        number {
            removeLeadingZeros = true
        }
    }
    println("Previous value: ${previousValue.toHexString(hexFormat)}")
    println("Current value: ${current.toHexString(hexFormat)}")
    println("Expected status flag set: ${current and 0xBAD != 0xBADL}")
}