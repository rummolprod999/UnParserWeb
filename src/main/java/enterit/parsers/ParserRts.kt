package enterit.parsers

import enterit.downloadFromUrl
import enterit.logger
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class ParserRts : Iparser {
    private val baseUrl = "http://corp.rts-tender.ru/?FilterData.PageSize=100&FilterData.PageIndex=1&FilterData.PageCount=1"
    private val maxPage = 2
    override fun parser() = (1..maxPage)
            .map { "http://corp.rts-tender.ru/?FilterData.PageSize=100&FilterData.PageIndex=$it&FilterData.PageCount=1" }
            .forEach {
                try {
                    parserPage(it)
                } catch (e: Exception) {
                    logger("Error in ParserRts.parser function", e.stackTrace, e)
                }
            }

    private fun parserPage(url: String) {
        val stPage = downloadFromUrl(url)
        if (stPage == "") {
            logger("Gets empty string ParserGpn", url)
            return
        }
        val html = Jsoup.parse(stPage)
        val tenders = html.select("table.purchase-card")
        if (tenders.isEmpty()) {
            logger("Gets empty list tenders", url)
        }
        tenders.forEach<Element> { t ->
            try {
                val urlT = t.selectFirst("span a")?.attr("href")?.trim { it <= ' ' } ?: ""
                println(urlT)
            } catch (e: Exception) {
                logger("error in ParserRts.parserPage()", e.stackTrace, e)
            }
        }
    }
}