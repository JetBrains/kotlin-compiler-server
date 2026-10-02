class User(val name: String) {
    inner class Age(val value: Int) {
        fun printInfo() {
            
            // Refers to the value property of the current Age object
            println(this.value)
            // 22
            
            // Refers to the name property of the outer User object 
            println(this@User.name)
            // Jane Doe
        }
    }
}

fun main() {
    val user = User("Jane Doe")
    val age = user.Age(22)
    age.printInfo()
}