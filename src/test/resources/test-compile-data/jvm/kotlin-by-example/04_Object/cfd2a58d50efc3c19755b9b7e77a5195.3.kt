object DoAuth {                                                 //1 
    fun takeParams(username: String, password: String) {        //2 
        println("input Auth parameters = $username:$password")
    }
}

fun main(){
    DoAuth.takeParams("foo", "qwerty")                          //3
}
