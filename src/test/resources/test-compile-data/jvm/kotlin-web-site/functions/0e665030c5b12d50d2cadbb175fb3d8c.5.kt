data class Person(val name: String)

val friendGroups = listOf(
    listOf(Person("Alice"), Person("Bob")),
    listOf(Person("Charlie"), Person("Diana"), Person("Eve")),
    listOf(Person("Frank"))
)

fun findLargestGroupOfFriends(): List<Person> {
    return friendGroups.maxByOrNull { it.size } ?: emptyList()
}

fun main() {
    val largestGroup = findLargestGroupOfFriends()

    println(largestGroup.map { it.name })
    // [Charlie, Diana, Eve]
}