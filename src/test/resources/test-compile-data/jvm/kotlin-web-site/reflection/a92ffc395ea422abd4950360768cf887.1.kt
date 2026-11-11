import kotlin.reflect.KClass

class User(val name: String)

open class Language
class Kotlin : Language()

fun main() {
    // Returns a KClass that represents User.
    val userClass: KClass<User> = User::class
    println(userClass.simpleName)
    // User
    
    val language: Language = Kotlin()

    // Returns a KClass that represents
    // the concrete class of the value at runtime
    val runtimeClass: KClass<out Language> = language::class
    println(runtimeClass.simpleName)
    // Kotlin
}