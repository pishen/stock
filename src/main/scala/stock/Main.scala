package stock

import better.files._
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import minitime._
import org.jsoup._
import scalaj.http._
import scala.collection.JavaConverters._
import scala.util.{Random,Try,Failure,Success}

object Main {
  val userAgents = Seq(
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36",
    "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36",
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2403.157 Safari/537.36",
    "Mozilla/5.0 (Windows NT 5.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36",
    "Mozilla/5.0 (Windows NT 6.2; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36",
    "Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.133 Safari/537.36",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36",
    "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.133 Safari/537.36",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.87 Safari/537.36",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.84 Safari/537.36",
    "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.87 Safari/537.36"
  )

  case class Proxy(host: String, port: Int)
  def getProxies() = {
    val doc = Jsoup.connect("https://free-proxy-list.net").get()
    doc
      .select("#proxylisttable > tbody > tr")
      .asScala
      .flatMap { tr =>
        val tds = tr.children.asScala
        if (tds(4).text == "elite proxy") {
          Some(Proxy(tds(0).text, tds(1).text.toInt))
        } else None
      }
      .toSet
  }

  @scala.annotation.tailrec
  def prepareTwse(date: LocalDate, proxies: Set[Proxy]): Set[Proxy] = {
    require(proxies.size >= 10, "proxies size < 10")
    val file = File(s"data/twse/$date.json")
    if (!file.exists) {
      val userAgent = Random.shuffle(userAgents.toSeq).head
      val proxy = Random.shuffle(proxies.toSeq).head
      val req = Http("http://www.twse.com.tw/exchangeReport/MI_INDEX")
        .params(
          "date" -> date.format(DateTimeFormatter.BASIC_ISO_DATE),
          "response" -> "json",
          "type" -> "ALLBUT0999"
        )
        .copy(headers = Seq.empty[(String, String)])
        .header("User-Agent", userAgent)
        .proxy(proxy.host, proxy.port)

      println(s"Downloading $date")
      val respTry = Try(req.asString)
      Thread.sleep((Random.nextInt(7) + 3) * 1000)
      respTry match {
        case Success(resp) =>
          if (resp.code == 200) {
            if (resp.body.size > 0) {
              file.overwrite(resp.body)
              proxies
            } else {
              println(s"Body size = 0, retry")
              prepareTwse(date, proxies)
            }
          } else if (resp.code == 504) {
            println(s"Gateway Timeout, remove $proxy, new size: ${proxies.size - 1}")
            prepareTwse(date, proxies - proxy)
          } else {
            throw new Exception(s"Bad Response $req $resp")
          }
        case Failure(e: java.net.SocketTimeoutException) =>
          println(s"connect timed out, remove $proxy, new size: ${proxies.size - 1}")
          prepareTwse(date, proxies - proxy)
        case Failure(e: java.net.ConnectException) =>
          println(s"Connection refused, remove $proxy, new size: ${proxies.size - 1}")
          prepareTwse(date, proxies - proxy)
        case Failure(e: java.io.IOException) =>
          println(s"IOException, remove $proxy, new size: ${proxies.size - 1}")
          prepareTwse(date, proxies - proxy)
        case Failure(e) =>
          throw new Exception(s"Unknown Error from $req", e)
      }
    } else proxies
  }

  def main(args: Array[String]): Unit = {
    println("Prepare proxies...")
    System.setProperty("https.protocols", "TLSv1.1,TLSv1.2")
    val proxies = getProxies()

    println("Prepare TWSE data...")
    val startDate = LocalDate.parse("2016-01-01")
    val endDate = LocalDate.parse("2016-12-31")
    Dsl.mkdirs(File("data/twse"))
    val dates = (startDate to endDate).filterNot { d =>
      d.getDayOfWeek == DayOfWeek.SUNDAY || d.getDayOfWeek == DayOfWeek.SATURDAY
    }
    dates.foldLeft(proxies)((proxies, date) => prepareTwse(date, proxies))
  }
}
