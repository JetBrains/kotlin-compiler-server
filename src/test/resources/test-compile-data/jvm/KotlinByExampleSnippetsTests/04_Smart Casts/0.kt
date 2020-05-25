import java.time.LocalDate
import java.time.chrono.ChronoLocalDate

fun main() {
//sampleStart
    val date: ChronoLocalDate? = LocalDate.now()    // 1
    
    if (date != null) {
        println(date.isLeapYear)                    // 2
    }
    
    if (date != null && date.isLeapYear) {          // 3
        println("It's a leap year!")
    }
    
    if (date == null || !date.isLeapYear) {         // 4
        println("There's no Feb 29 this year...")
    }
    
    if (date is LocalDate) {
        val month = date.monthValue                 // 5
        println(month)
    }
//sampleEnd
}
