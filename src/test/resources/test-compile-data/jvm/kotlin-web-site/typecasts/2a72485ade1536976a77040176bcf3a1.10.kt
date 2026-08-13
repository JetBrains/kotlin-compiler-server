interface Animal {
    fun makeSound()
}

class Dog : Animal {
    // Implements behavior for makeSound()
    override fun makeSound() {
        println("Dog says woof!")
    }
}

fun printAnimalInfo(animal: Animal) {
    animal.makeSound()
}

fun main() {
    val dog = Dog()
    // Upcasts Dog instance to Animal
    printAnimalInfo(dog)  
    // Dog says woof!
}