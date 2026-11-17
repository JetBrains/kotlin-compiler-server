fun main() {
//sampleStart
    println("Range with an inclusive upper bound:")
    for (i in 1..6) {
        print(i)
    }
    // Range with an inclusive upper bound:
    // 123456
  
    println("\nRange with an exclusive upper bound:")
    for (i in 1..<6) {
        print(i)
    }
    // Range with an exclusive upper bound:
    // 12345
  
    println("\nReverse order in steps of 2:")
    for (i in 6 downTo 0 step 2) {
        print(i)
    }
    // Reverse order in steps of 2:
    // 6420
//sampleEnd
}