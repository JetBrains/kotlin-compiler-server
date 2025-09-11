data class Employee(val name: String, val roles: MutableList<String>)

fun main() {
    val original = Employee("Jamie", mutableListOf("developer"))
    val duplicate = original.copy()

    duplicate.roles.add("team lead")

    println(original) 
    // Employee(name=Jamie, roles=[developer, team lead])
    println(duplicate) 
    // Employee(name=Jamie, roles=[developer, team lead])
}