package enterit.parsers

import enterit.downloadFromUrl
import enterit.logger
import enterit.tenders.TenderGpn
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class ParserGpn : Iparser {
    private val baseUrls = listOf(
        "https://zakupki.gazprom-neft.ru/tenderix/?PAGE=",
        "https://zakupki.gazprom-neft.ru/tenderix/prequalification.php?PAGE="
    )

    companion object BaseTen {
        const val BaseT = "https://zakupki.gazprom-neft.ru"
    }

    private val maxPage = 15
    override fun parser() = baseUrls.forEach { baseUrl ->
        (1..maxPage)
            .map { "$baseUrl$it" }
            .forEach {
                try {
                    parserPage(it)
                } catch (e: Exception) {
                    logger("Error in ParserGpn.parser function", e.stackTrace, e)
                }
            }
    }

    private fun parserPage(url: String) {
        val stPage = downloadFromUrl(url)
        if (stPage == "") {
            logger("Gets empty string ParserGpn", url)
            return
        }
        val html = Jsoup.parse(stPage)
        val tenders = html.select("div.purchase-container")
        if (tenders.isEmpty()) {
            logger("Gets empty list tenders", url)
        }
        tenders.forEach<Element> { t ->
            try {
                val status = t.selectFirst("div.purchase-status")?.ownText()?.trim { it <= ' ' } ?: ""
                val urlT = t.selectFirst("div.purchase-number a")?.attr("href")?.trim { it <= ' ' } ?: ""
                val urlTend = "$BaseT$urlT"
                val tt = TenderGpn(status, urlTend)
                tt.parsing()
            } catch (e: Exception) {
                logger("error in ParserGpn.parserPage()", e.stackTrace, e)
            }
        }
    }
}