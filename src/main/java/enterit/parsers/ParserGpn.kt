package enterit.parsers

import enterit.downloadFromUrl
import enterit.logger
import enterit.tenders.TenderGpn
import enterit.tenders.TenderGpnAnn
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class ParserGpn : Iparser {
    private val baseUrls = listOf(
        "https://zakupki.gazprom-neft.ru/tenderix/?PAGE=",
        "https://zakupki.gazprom-neft.ru/tenderix/prequalification.php?PAGE="
    )

    private val baseUrlsAnn = listOf(
        "https://zakupki.gazprom-neft.ru/tenderix/announcements/"
    )

    private val baseUrlsMp = listOf(
        "https://zakupki.gazprom-neft.ru/msp/fast/?PAGE="
    )

    companion object BaseTen {
        const val BaseT = "https://zakupki.gazprom-neft.ru"
    }

    private val maxPage = 15
    override fun parser() {
        baseUrlsMp.forEach { baseUrl ->
            (1..3)
                .map { "$baseUrl$it" }
                .forEach {
                    try {
                        parserPageMp(it)
                    } catch (e: Exception) {
                        logger("Error in ParserGpn.parser function", e.stackTrace, e)
                    }
                }
        }
        baseUrls.forEach { baseUrl ->
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
        baseUrlsAnn.forEach {
            try {
                parserPageAnn(it)
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

    private fun parserPageMp(url: String) {
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
                val urlTend = "https://zakupki.gazprom-neft.ru/msp/fast/$urlT"
                val tt = TenderGpn(status, urlTend)
                tt.parsing()
            } catch (e: Exception) {
                logger("error in ParserGpn.parserPage()", e.stackTrace, e)
            }
        }
    }

    private fun parserPageAnn(url: String) {
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
                if (!urlT.contains("announcements")) {
                    return
                }
                val urlTend = "$BaseT$urlT"
                val tt = TenderGpnAnn(status, urlTend)
                tt.parsing()
            } catch (e: Exception) {
                logger("error in ParserGpn.parserPage()", e.stackTrace, e)
            }
        }
    }
}