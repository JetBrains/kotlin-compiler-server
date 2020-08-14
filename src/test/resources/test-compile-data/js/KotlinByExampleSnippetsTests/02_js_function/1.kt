fun main(){
//sampleStart
  val json = js("{}")               // 1
  json.name = "Jane"                // 2
  json.hobby = "movies"
  
  println(JSON.stringify(json))     // 3
//sampleEnd
}