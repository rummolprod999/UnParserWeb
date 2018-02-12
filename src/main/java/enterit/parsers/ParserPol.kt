package enterit.parsers

import enterit.downloadFromUrl
import enterit.logger
import enterit.tenders.TenderPol
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class ParserPol : Iparser {
    companion object BaseTen {
        const val BaseT = "http://tenders.polyusgold.com/purchases/"
    }

    private val maxPage = 8
    private val baseUrl = "http://tenders.polyusgold.com/purchases/?&PAGEN_1="
    override fun parser() = (maxPage downTo 1)
            .map { "$baseUrl$it" }
            .forEach {
                try {
                    parserPage(it)
                } catch (e: Exception) {
                    logger("Error in ParserPol.parser function", e.stackTrace, e)
                }
            }

    private fun parserPage(url: String) {
        val stPage = downloadFromUrl(url)
        if (stPage == "") {
            logger("Gets empty string ParserPol", url)
            return
        }
        val html = Jsoup.parse(stPage)
        val tenders = html.select("table.purchases-table > tbody > tr")
        if (tenders.isEmpty()) {
            logger("Gets empty list tenders", url)
        }
        tenders.forEach<Element> { t ->
            try {

                val urlT = t.selectFirst("tr div.purchase-title > a")?.attr("href")?.trim { it <= ' ' } ?: ""
                val urlTend = "${ParserPol.BaseT}$urlT"
                val tt = TenderPol(urlTend)
                tt.parsing()
            } catch (e: Exception) {
                logger("error in ParserPol.parserPage()", e.stackTrace, e)
            }
        }
    }
}