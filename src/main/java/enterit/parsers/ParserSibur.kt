package enterit.parsers

import enterit.downloadFromUrl
import enterit.logger
import enterit.tenders.TenderSibur
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class ParserSibur : Iparser {
    private val maxPage = 50

    companion object BaseTen {
        const val BaseT = "https://b2b.sibur.ru/pages_new_ru/exchange/"
    }

    override fun parser() = (1..maxPage)
        .map { "https://b2b.sibur.ru/pages_new_ru/exchange/exchange.jsp?page=$it&disp_status=0" }
        .forEach {
            try {
                parserPage(it)
            } catch (e: Exception) {
                logger("Error in ParserSibur.parser function", e.stackTrace, e)
            }
        }

    private fun parserPage(url: String) {
        val stPage = downloadFromUrl(url, wt = 10000)
        if (stPage == "") {
            logger("Gets empty string ParserSibur", url)
            return
        }
        val html = Jsoup.parse(stPage)
        val tenders = html.select("div.ince > div.odd, div.even")
        if (tenders.isEmpty()) {
            logger("Gets empty list tenders", url)
        }
        tenders.forEach<Element> { t ->
            try {
                val urlT = t.selectFirst("a.big")?.attr("href")?.trim { it <= ' ' }
                    ?: ""
                val urlTend = "$BaseT$urlT"
                val currency = t.selectFirst("div.drei div.red")?.ownText()?.trim { it <= ' ' }
                    ?: ""
                val purNum = t.selectFirst("div.ein")?.ownText()?.trim { it <= ' ' }
                    ?: ""
                val tt = TenderSibur(urlTend, purNum, currency)
                tt.parsing()
            } catch (e: Exception) {
                logger("error in ParserSibur.parserPage()", e.stackTrace, e)
            }
        }
    }
}