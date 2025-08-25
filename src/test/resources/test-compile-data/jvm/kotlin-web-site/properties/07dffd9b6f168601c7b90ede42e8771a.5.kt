class Scoreboard {
    var score: Int = 0
        set(value) {
            field = value
            // Adds logging when updating the value
            println("Score updated to $field")
        }
}

fun main() {
    val board = Scoreboard()
    board.score = 10  
    // Score updated to 10
    board.score = 20  
    // Score updated to 20
}