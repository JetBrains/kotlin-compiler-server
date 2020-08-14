fun main() {
//sampleStart    
println(listOf("aaa", "bb", "c").sortedWith(compareBy { it.length }))
//sampleEnd
}
