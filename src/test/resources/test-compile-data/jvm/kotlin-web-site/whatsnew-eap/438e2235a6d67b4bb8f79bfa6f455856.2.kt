fun main() {
    val myString = "Kotlin is awesome!"
    val destinationArray = CharArray(myString.length) 
   
    // Convert the string and store it in the destinationArray:
    myString.toCharArray(destinationArray) 
   
    for (char in destinationArray) {
        print("$char ")
    }
}