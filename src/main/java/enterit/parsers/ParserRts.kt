package enterit.parsers

import enterit.downloadFromUrl
import enterit.logger
import org.jsoup.Jsoup

class ParserRts : Iparser {
    private val baseUrl = "http://corp.rts-tender.ru/?FilterData.PageSize=100&FilterData.PageIndex=1&FilterData.PageCount="
    private val maxPage = 2
    override fun parser() = (1..maxPage)
            .map { "$baseUrl$it" }
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
        val tenders = html.select("table.result-table > tbody > tr:qt(0)")
        if (tenders.isEmpty()) {
            logger("Gets empty list tenders", url)
        }
    }
}