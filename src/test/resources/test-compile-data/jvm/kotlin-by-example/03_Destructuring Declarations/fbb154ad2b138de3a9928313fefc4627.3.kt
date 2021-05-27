class Pair<K, V>(val first: K, val second: V) {  // 1
    operator fun component1(): K {              
        return first
    }

    operator fun component2(): V {              
        return second
    }
}

fun main() {
    val (num, name) = Pair(1, "one")             // 2

    println("num = $num, name = $name")
}