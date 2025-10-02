// Class with a primary constructor 
// including default values for name and age
class Person(val name: String = "John", var age: Int = 30)

fun main() {
    // Creates an instance using default values
    val person = Person()
    println("Name: ${person.name}, Age: ${person.age}")
    // Name: John, Age: 30
}