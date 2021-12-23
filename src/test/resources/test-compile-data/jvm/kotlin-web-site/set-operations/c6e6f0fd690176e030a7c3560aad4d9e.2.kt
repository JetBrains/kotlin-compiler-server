fun main() {
//sampleStart
    val list1 = listOf(1, 1, 2 ,3, 5, 8, -1)
    val list2 = listOf(1, 1, 2, 2 ,3, 5)
    println(list1 intersect list2) // result on two lists is a Set
    println(list1 union list2)     // equal elements are merged into one
//sampleEnd
}