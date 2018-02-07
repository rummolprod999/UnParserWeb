package enterit.parsers

import enterit.downloadFromUrl
import enterit.logger
import enterit.tenders.TenderGpn
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class ParserGpn : Iparser {
    val BaseUrl = "http://zakupki.gazprom-neft.ru/tenderix/?PAGE="
    val BaseT = "http://zakupki.gazprom-neft.ru"
    val maxPage = 15
    override fun parser() = (1..maxPage)
            .map { "$BaseUrl$it" }
            .forEach {
                try {
                    parserPage(it)
                } catch (e: Exception) {
                    logger("Error in ParserGpn.parser function", e.stackTrace, e)
                }
            }

    private fun parserPage(url: String) {
        val stPage = downloadFromUrl(url)
        if (stPage == "") {
            logger("Gets empty string ParserGpn", url)
            return
        }
        val html = Jsoup.parse(stPage)
        val tenders = html.select("article[id]")
        if (tenders.isEmpty()) {
            logger("Gets empty list tenders", url)
        }
        tenders.forEach<Element> { t ->
            try {
                val status = t.selectFirst("span:contains(Статус:) ~ span")?.ownText()?.trim { it <= ' ' } ?: ""
                val urlT = t.selectFirst("a:containsOwn(Подробнее)")?.attr("href")?.trim { it <= ' ' } ?: ""
                val urlTend = "$BaseT$urlT"
                val tt = TenderGpn(status, urlTend)
                tt.parsing()
            } catch (e: Exception) {
                logger("error in ParserGpn.parserPage()", e.stackTrace, e)
            }
        }
    }
}