package stock

import org.scalajs.dom.document
import scala.scalajs.js
import scalatags.JsDom.all._

object Main {
  def main(args: Array[String]): Unit = {
    println("Hello Stock!")

    val lines = canvas(width := "600", height := "400").render



    val root = div(cls := "container", lines).render
    document.querySelector("#root").appendChild(root)
  }
}
