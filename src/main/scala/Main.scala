import scala.concurrent._
import scala.concurrent.duration._
import scala.xml._
import java.util.concurrent.TimeoutException

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.coding.{ Gzip, Deflate, NoCoding }
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.settings.ConnectionPoolSettings

object Main {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  private val uriList = Seq(
    "https://news.yahoo.co.jp/pickup/rss.xml",
    "http://rss.asahi.com/rss/asahi/national.rdf",
    "https://www3.nhk.or.jp/rss/news/cat0.xml"
  )

  def main(args: Array[String]): Unit = {
    val timeout = 10.0

    val reqList: Seq[Future[HttpResponse]] = for (uri <- uriList) yield request(uri)

    val futureSeq = reqList.map { req =>
      req.map(decodeResponse(_)).map(postProcess(_)).recover {
        case e =>
          println(e.getMessage)
          Seq()
      }
    }

    val result: Seq[String] = try {
      Await.result(scala.concurrent.Future.sequence(futureSeq), Duration(timeout, SECONDS))
        .flatMap(_.asInstanceOf[Seq[String]])
    } catch {
      case e: TimeoutException => {
        println("timed out !: %s".format(e))
        Seq()
      }
    }

    system.terminate

    println("result: total %d entries found.".format(result.size))
  }

  private def postProcess(res: HttpResponse): Seq[String] = {
    val content = extractBody(res)

    val xml = XML.loadString(content)
    val titles = for (t <- xml \\ "title") yield t.text

    println("success: %d titles found (%d bytes)".format(titles.size, content.length))

    titles.toSeq
  }

  private def extractBody(res: HttpResponse): String = {
    val body: Future[String] = Unmarshal(res.entity).to[String]

    Await.result(body, Duration.Inf)
  }

  private def request(uri: String): Future[HttpResponse] = {
    val req = HttpRequest(GET, uri = Uri(uri))
      .withHeaders(
        RawHeader("Accept-Encoding", "gzip,deflate")
      )

    Http().singleRequest(req)
    // Http().singleRequest(req, settings = connectionPoolSettings) // via https proxy: unstable
  }

  // https://stackoverflow.com/questions/45369615/route-akka-http-request-through-a-proxy
  private lazy val connectionPoolSettings: ConnectionPoolSettings = {
    import akka.http.scaladsl.ClientTransport
    import java.net.InetSocketAddress

    case class ProxyConfig(host: String, port: Int)

    val proxyConfig = Some(ProxyConfig("localhost", 8888))
    val clientTransport =
      proxyConfig.map(p => ClientTransport.httpsProxy(InetSocketAddress.createUnresolved(p.host, p.port)))
        .getOrElse(ClientTransport.TCP)

    ConnectionPoolSettings(system).withTransport(clientTransport)
  }

  private def decodeResponse(res: HttpResponse): HttpResponse = {
    val decoder = res.encoding match {
      case HttpEncodings.gzip =>
        Gzip
      case HttpEncodings.deflate =>
        Deflate
      case HttpEncodings.identity =>
        NoCoding
      case _ ⇒
        NoCoding
    }

    decoder.decodeMessage(res)
  }
}
