package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import org.junit.jupiter.api.Test

class ConvertToJsRunnerTest : BaseExecutorTest() {

  @Test
  fun `base execute test`() {
    runJs(
      code = "fun main() {\n println(\"Hello, world!!!\")\n}",
      contains = "println('Hello, world!!!');"
    )
  }

  @Test
  fun `base execute test multi`() {
    runJs(
      code = listOf(
        "import cat.Cat\n\nfun main(args: Array<String>) {\nval cat = Cat(\"Kitty\")\nprintln(cat.name)\n}",
        "package cat\n    class Cat(val name: String)"
      ),
      contains = "var cat = new Cat('Kitty');"
    )
  }

  @Test
  fun `canvas with jquery test`() {
    runJs(
      code = "package fancylines\n\n\nimport org.w3c.dom.CanvasRenderingContext2D\nimport org.w3c.dom.HTMLCanvasElement\nimport kotlinx.browser.document\nimport kotlinx.browser.window\nimport kotlin.random.Random\n\n\n\nval canvas = initalizeCanvas()\nfun initalizeCanvas(): HTMLCanvasElement {\n    val canvas = document.createElement(\"canvas\") as HTMLCanvasElement\n    val context = canvas.getContext(\"2d\") as CanvasRenderingContext2D\n    context.canvas.width  = window.innerWidth.toInt();\n    context.canvas.height = window.innerHeight.toInt();\n    document.body!!.appendChild(canvas)\n    return canvas\n}\n\nclass FancyLines() {\n    val context = canvas.getContext(\"2d\") as CanvasRenderingContext2D\n    val height = canvas.height\n    val width = canvas.width\n    var x = width * Random.nextDouble()\n    var y = height * Random.nextDouble()\n    var hue = 0;\n\n    fun line() {\n        context.save();\n\n        context.beginPath();\n\n        context.lineWidth = 20.0 * Random.nextDouble();\n        context.moveTo(x, y);\n\n        x = width * Random.nextDouble();\n        y = height * Random.nextDouble();\n\n        context.bezierCurveTo(width * Random.nextDouble(), height * Random.nextDouble(),\n                width * Random.nextDouble(), height * Random.nextDouble(), x, y);\n\n        hue += (Random.nextDouble() * 10).toInt();\n\n        context.strokeStyle = \"hsl(\$hue, 50%, 50%)\";\n\n        context.shadowColor = \"white\";\n        context.shadowBlur = 10.0;\n\n        context.stroke();\n\n        context.restore();\n    }\n\n    fun blank() {\n        context.fillStyle = \"rgba(255,255,1,0.1)\";\n        context.fillRect(0.0, 0.0, width.toDouble(), height.toDouble());\n    }\n\n    fun run() {\n        window.setInterval({ line() }, 40);\n        window.setInterval({ blank() }, 100);\n    }\n}\nfun main(args: Array<String>) {\n     FancyLines().run()\n}",
      contains = "this.context = Kotlin.isType(tmp\$ = canvas.getContext('2d'), CanvasRenderingContext2D) ? tmp\$ : throwCCE();",
    )
  }
}

class ConvertToJsIrRunnerTest : BaseExecutorTest() {
  @Test
  fun `base execute test`() {
    runJsIr(
      code = "fun main() {\n println(\"Hello, world!!!\")\n}",
      contains = "println('Hello, world!!!');"
    )
  }

  @Test
  fun `base execute test multi`() {
    runJsIr(
      code = listOf(
        "import cat.Cat\n\nfun main(args: Array<String>) {\nval cat = Cat(\"Kitty\")\nprintln(cat.name)\n}",
        "package cat\n    class Cat(val name: String)"
      ),
      contains = "var cat = new Cat('Kitty');"
    )
  }
}