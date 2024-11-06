fun main() {
//sampleStart
    val list = listOf("kot", "lin")
    val lettersList = list.flatMap { it.asSequence() }
    val lettersSeq = list.asSequence().flatMap { it.toList() }    
//sampleEnd
    println(lettersList)
    println(lettersSeq.toList())
}