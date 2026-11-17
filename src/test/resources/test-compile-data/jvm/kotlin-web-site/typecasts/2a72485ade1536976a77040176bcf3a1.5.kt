fun processInput(data: Any) {
    when (data) {
        // data is automatically cast to Int
        is Int -> println("Log: Assigned new ID ${data + 1}")
        // data is automatically cast to String
        is String -> println("Log: Received message \"$data\"")
        // data is automatically cast to IntArray
        is IntArray -> println("Log: Processed scores, total = ${data.sum()}")
    }
}

fun main() {
    processInput(1001)
    // Log: Assigned new ID 1002
    processInput("System rebooted")
    // Log: Received message "System rebooted"
    processInput(intArrayOf(10, 20, 30))
    // Log: Processed scores, total = 60
}