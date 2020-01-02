package com.compiler.server

import com.compiler.server.generator.TestProjectRunner
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class ConvertToJsRunnerTest {
  @Autowired
  private lateinit var testRunner: TestProjectRunner

  @Test
  fun `base execute test`() {
    testRunner.runJs(
      code = "fun main() {\n println(\"Hello, world!!!\")\n}",
      contains = "println('Hello, world!!!');"
    )
  }

  @Test
  fun `base execute test multi`() {
    testRunner.multiRunJs(
      code = listOf(
        "import cat.Cat\n\nfun main(args: Array<String>) {\nval cat = Cat(\"Kitty\")\nprintln(cat.name)\n}",
        "package cat\n    class Cat(val name: String)"
      ),
      contains = "var cat = new Cat('Kitty');"
    )
  }

  @Test
  fun `canvas with jquery test`() {
    testRunner.runJs(
      code = "package fancylines\n\n\nimport jquery.*\nimport org.w3c.dom.CanvasRenderingContext2D\nimport org.w3c.dom.HTMLCanvasElement\nimport kotlin.browser.document\nimport kotlin.browser.window\nimport kotlin.js.Math\n\n\n\nval canvas = initalizeCanvas()\nfun initalizeCanvas(): HTMLCanvasElement {\n    val canvas = document.createElement(\"canvas\") as HTMLCanvasElement\n    val context = canvas.getContext(\"2d\") as CanvasRenderingContext2D\n    context.canvas.width  = window.innerWidth.toInt();\n    context.canvas.height = window.innerHeight.toInt();\n    document.body!!.appendChild(canvas)\n    return canvas\n}\n\nclass FancyLines() {\n    val context = canvas.getContext(\"2d\") as CanvasRenderingContext2D\n    val height = canvas.height\n    val width = canvas.width\n    var x = width * Math.random()\n    var y = height * Math.random()\n    var hue = 0;\n\n    fun line() {\n        context.save();\n\n        context.beginPath();\n\n        context.lineWidth = 20.0 * Math.random();\n        context.moveTo(x, y);\n\n        x = width * Math.random();\n        y = height * Math.random();\n\n        context.bezierCurveTo(width * Math.random(), height * Math.random(),\n                width * Math.random(), height * Math.random(), x, y);\n\n        hue += (Math.random() * 10).toInt();\n\n        context.strokeStyle = \"hsl(\$hue, 50%, 50%)\";\n\n        context.shadowColor = \"white\";\n        context.shadowBlur = 10.0;\n\n        context.stroke();\n\n        context.restore();\n    }\n\n    fun blank() {\n        context.fillStyle = \"rgba(255,255,1,0.1)\";\n        context.fillRect(0.0, 0.0, width.toDouble(), height.toDouble());\n    }\n\n    fun run() {\n        window.setInterval({ line() }, 40);\n        window.setInterval({ blank() }, 100);\n    }\n}\nfun main(args: Array<String>) {\n     FancyLines().run()\n}",
      contains = "this.context = Kotlin.isType(tmp\$ = canvas.getContext('2d'), CanvasRenderingContext2D) ? tmp\$ : throwCCE();"
    )
  }
}