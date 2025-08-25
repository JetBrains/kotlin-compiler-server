class Address {
    var name: String = "Holmes, Sherlock"
    var street: String = "Baker"
    var city: String = "London"
}

interface ContactInfo {
    val email: String
}

object Company {
    var name: String = "Detective Inc."
    val country: String = "UK"
}

class PersonContact : ContactInfo {
    override val email: String = "sherlock@example.com"
}

//sampleStart
fun copyAddress(address: Address): Address {
    val result = Address()
    // Accesses properties in the result instance
    result.name = address.name
    result.street = address.street
    result.city = address.city
    return result
}

fun main() {
    val sherlockAddress = Address()
    val copy = copyAddress(sherlockAddress)
    // Accesses properties in the copy instance
    println("Copied address: ${copy.name}, ${copy.street}, ${copy.city}")
    // Copied address: Holmes, Sherlock, Baker, London

    // Accesses properties in the Company object
    println("Company: ${Company.name} in ${Company.country}")
    // Company: Detective Inc. in UK
    
    val contact = PersonContact()
    // Access properties in the contact instance
    println("Email: ${contact.email}")
    // Email: sherlock@email.com
}
//sampleEnd