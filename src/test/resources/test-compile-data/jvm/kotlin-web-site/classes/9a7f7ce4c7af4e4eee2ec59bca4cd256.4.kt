// Class with a primary constructor 
// including default values for name and age
class Person(
    val name: String = "John",
    var age: Int = 30
) {
    // Initializes the description property 
    // from the primary constructor parameters
    val description: String = "Name: $name, Age: $age"
}

fun main() {
    // Creates an instance of the Person class
    val person = Person()
    // Accesses the description property
    println(person.description)
    // Name: John, Age: 30
}