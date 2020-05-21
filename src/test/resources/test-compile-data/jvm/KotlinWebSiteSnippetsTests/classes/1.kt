//sampleStart
class Constructors {
    init {
        println("Init block")
    }

    constructor(i: Int) {
        println("Constructor")
    }
}
//sampleEnd

fun main() {
    Constructors(1)
}