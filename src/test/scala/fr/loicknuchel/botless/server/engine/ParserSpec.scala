package fr.loicknuchel.botless.server.engine

import java.time.{OffsetDateTime, ZoneOffset}

import cats.implicits.catsSyntaxValidatedIdBinCompat0
import fr.loicknuchel.botless.server.domain.HttpVerb.{GET, HEAD}
import fr.loicknuchel.botless.server.domain._
import fr.loicknuchel.botless.server.engine.Parser.ParsingError._
import fr.loicknuchel.botless.shared.domain.RawLog
import fr.loicknuchel.botless.testingutils.BaseSpec
import fr.loicknuchel.botless.testingutils.Fixtures.ip

class ParserSpec extends BaseSpec {
  private val dateStr = "12/Dec/2015:18:25:11 +0100"
  private val date = OffsetDateTime.of(2015, 12, 12, 18, 25, 11, 0, ZoneOffset.ofHours(1))

  describe("Parser") {
    describe("parse") {
      it("should parse a correct access logs") {
        Parser.parse(RawLog("109.169.248.247 - - [" + dateStr + "] \"GET /administrator/ HTTP/1.1\" 200 4263 \"-\" \"Mozilla/5.0 (Windows NT 6.0; rv:34.0) Gecko/20100101 Firefox/34.0\" \"-\"")) shouldBe
          ParsedLog(ip(109, 169, 248, 247), "-", "-", date, GET, UrlRequest("/administrator/"), HttpProtocol("HTTP/1.1"), HttpStatus(200), Some(ResponseSize(4263)), Referer("-"), UserAgent("Mozilla/5.0 (Windows NT 6.0; rv:34.0) Gecko/20100101 Firefox/34.0"), "-").validNec
      }
      it("should parse a log with empty referer and user agent") {
        Parser.parse(RawLog("88.198.140.4 - - [" + dateStr + "] \"GET /robots.txt HTTP/1.1\" 200 304 \"\" \"\" \"-\"")) shouldBe
          ParsedLog(ip(88, 198, 140, 4), "-", "-", date, GET, UrlRequest("/robots.txt"), HttpProtocol("HTTP/1.1"), HttpStatus(200), Some(ResponseSize(304)), Referer(""), UserAgent(""), "-").validNec
      }
      it("should parse a log with escaped chars in referer and user agent") {
        // referer escaping
        Parser.parse(RawLog("79.134.218.10 - - [" + dateStr + "] \"GET / HTTP/1.1\" 200 10439 \"<a href=\\\"http://forum.windowsfaq.ru/\\\">Forum.WindowsFAQ.ru</a>\" \"<a href=\\\"http://forum.windowsfaq.ru/\\\">Forum.WindowsFAQ.ru</a>\" \"-\"")) shouldBe
          ParsedLog(ip(79, 134, 218, 10), "-", "-", date, GET, UrlRequest("/"), HttpProtocol("HTTP/1.1"), HttpStatus(200), Some(ResponseSize(10439)), Referer("<a href=\\\"http://forum.windowsfaq.ru/\\\">Forum.WindowsFAQ.ru</a>"), UserAgent("<a href=\\\"http://forum.windowsfaq.ru/\\\">Forum.WindowsFAQ.ru</a>"), "-").validNec

        // user agent escaping
        Parser.parse(RawLog("81.7.16.13 - - [" + dateStr + "] \"GET / HTTP/1.1\" 200 10439 \"http://almhuette-raith.at/\" \"}__test|O:21:\\\"JDatabaseDriverMysqli\\\":3:{s:2:\\\"fc\\\";O:17:\\\"JSimplepieFactory\\\":0:{}s:21:\\\"\\\\0\\\\0\\\\0disconnectHandlers\\\";a:1:{i:0;a:2:{i:0;O:9:\\\"SimplePie\\\":5:{s:8:\\\"sanitize\\\";O:20:\\\"JDatabaseDriverMysql\\\":0:{}s:8:\\\"feed_url\\\";s:60:\\\"eval(base64_decode($_POST[111]));JFactory::getConfig();exit;\\\";s:19:\\\"cache_name_function\\\";s:6:\\\"assert\\\";s:5:\\\"cache\\\";b:1;s:11:\\\"cache_class\\\";O:20:\\\"JDatabaseDriverMysql\\\":0:{}}i:1;s:4:\\\"init\\\";}}s:13:\\\"\\\\0\\\\0\\\\0connection\\\";b:1;}\\xf0\\x9d\\x8c\\x86\" \"-\"")) shouldBe
          ParsedLog(ip(81, 7, 16, 13), "-", "-", date, GET, UrlRequest("/"), HttpProtocol("HTTP/1.1"), HttpStatus(200), Some(ResponseSize(10439)), Referer("http://almhuette-raith.at/"), UserAgent("}__test|O:21:\\\"JDatabaseDriverMysqli\\\":3:{s:2:\\\"fc\\\";O:17:\\\"JSimplepieFactory\\\":0:{}s:21:\\\"\\\\0\\\\0\\\\0disconnectHandlers\\\";a:1:{i:0;a:2:{i:0;O:9:\\\"SimplePie\\\":5:{s:8:\\\"sanitize\\\";O:20:\\\"JDatabaseDriverMysql\\\":0:{}s:8:\\\"feed_url\\\";s:60:\\\"eval(base64_decode($_POST[111]));JFactory::getConfig();exit;\\\";s:19:\\\"cache_name_function\\\";s:6:\\\"assert\\\";s:5:\\\"cache\\\";b:1;s:11:\\\"cache_class\\\";O:20:\\\"JDatabaseDriverMysql\\\":0:{}}i:1;s:4:\\\"init\\\";}}s:13:\\\"\\\\0\\\\0\\\\0connection\\\";b:1;}\\xf0\\x9d\\x8c\\x86"), "-").validNec
      }
      it("should parse a log with no response size") {
        Parser.parse(RawLog("40.77.167.38 - - [" + dateStr + "] \"GET /configuration.php-dist HTTP/1.1\" 304 - \"-\" \"Mozilla/5.0 (compatible; bingbot/2.0; +http://www.bing.com/bingbot.htm)\" \"-\"")) shouldBe
          ParsedLog(ip(40, 77, 167, 38), "-", "-", date, GET, UrlRequest("/configuration.php-dist"), HttpProtocol("HTTP/1.1"), HttpStatus(304), None, Referer("-"), UserAgent("Mozilla/5.0 (compatible; bingbot/2.0; +http://www.bing.com/bingbot.htm)"), "-").validNec
      }
      it("should parse a log with lowercase http verb") {
        Parser.parse(RawLog("113.161.24.118 - - [" + dateStr + "] \"Head //images/stories/movie.php HTTP/1.1\" 501 302 \"-\" \"BOT/0.1 (BOT for JCE)\" \"-\"")) shouldBe
          ParsedLog(ip(113, 161, 24, 118), "-", "-", date, HEAD, UrlRequest("//images/stories/movie.php"), HttpProtocol("HTTP/1.1"), HttpStatus(501), Some(ResponseSize(302)), Referer("-"), UserAgent("BOT/0.1 (BOT for JCE)"), "-").validNec
      }
      it("should parse a log with an invalid http verb") {
        Parser.parse(RawLog("112.175.184.9 - - [" + dateStr + "] \"T /index.php?option=com_jce&task=plugin&plugin=imgmanager&file=imgmanager&version=1576&cid=20 HTTP/1.1\" 200 24571 \"-\" \"BOT/0.1 (BOT for JCE)\" \"-\"")) shouldBe
          InvalidHttpVerb("T", HttpVerb.all.map(_.name)).invalidNec
      }
    }
    it("should parse a date") {
      Parser.parseDate(dateStr) shouldBe date.validNec
      Parser.parseDate("bad") shouldBe InvalidDate("bad", "dd/MMM/yyyy:HH:mm:ss Z").invalidNec
    }
  }
}
