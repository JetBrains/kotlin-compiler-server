 class WrappedText(val text: String) : Comparable<WrappedText> {
   override fun compareTo(other: WrappedText): Int =
       this.text compareTo other.text
}