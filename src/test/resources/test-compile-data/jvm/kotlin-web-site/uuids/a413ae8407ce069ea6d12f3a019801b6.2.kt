import kotlin.time.Instant
import kotlin.time.ExperimentalTime
import kotlin.uuid.Uuid

@OptIn(kotlin.uuid.ExperimentalUuidApi::class, ExperimentalTime::class)
fun main() {
    // Generates a version 4 UUID
    val idVersion4 = Uuid.generateV4()
    println(idVersion4)

    // Generates a version 7 UUID
    val idVersion7 = Uuid.generateV7()
    println(idVersion7)

    // Generates a version 7 UUID for the specified timestamp
    val timestamp = Instant.fromEpochMilliseconds(1757440583000L)
    val idVersion7SpecificTime = Uuid.generateV7NonMonotonicAt(timestamp)
    println(idVersion7SpecificTime)
}