package enterit.parsers

import enterit.downloadFromUrl
import enterit.formatterGpn
import enterit.getDateFromFormat
import enterit.logger
import enterit.tenders.TenderRfp
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.util.*

class ParserRfp : Iparser {
    private val baseUrl = "https://www.rfp.ltd/zakupki/?page="

    companion object BaseTen {
        const val BaseT = "https://www.rfp.ltd"
    }

    private val maxPage = 10

    override fun parser() = (1..maxPage)
            .map { "$baseUrl$it" }
            .forEach {
                try {
                    parserPage(it)
                } catch (e: Exception) {
                    logger("Error in ParserRfp.parser function", e.stackTrace, e)
                }
            }

    private fun parserPage(url: String) {
        val stPage = downloadFromUrl(url)
        if (stPage == "") {
            logger("Gets empty string ParserGpn", url)
            return
        }
        val html = Jsoup.parse(stPage)
        val tenders = html.select("table.tender_list tbody tr.tender")
        if (tenders.isEmpty()) {
            logger("Gets empty list tenders", url)
        }
        tenders.forEach<Element> { t ->
            try {
                parserTender(t)
            } catch (e: Exception) {
                logger("error in parserTender()", e.stackTrace, e)
            }
        }
    }

    private fun parserTender(t: Element) {
        val urlT = t.selectFirst("span:containsOwn(Наименование:) + a")?.attr("href")?.trim { it <= ' ' } ?: ""
        val urlTend = "$BaseT$urlT"
        val datePubTmp = t.selectFirst("div:containsOwn(Опубликовано:) + span")?.ownText()?.replace("в ", "")?.trim { it <= ' ' }
                ?: ""
        val dateEndTmp = t.selectFirst("div.margin_b > span:containsOwn(до) + span")?.ownText()?.replace("в ", "")?.trim { it <= ' ' }
                ?: ""
        val datePub = getDateFromFormat(datePubTmp, formatterGpn)
        val dateEnd = getDateFromFormat(dateEndTmp, formatterGpn)
        if (datePub == Date(0L) || dateEnd == Date(0L)) {
            logger("can not find datePub or dateEnd on page", urlT, datePubTmp, dateEndTmp)
            return
        }
        val status = t.selectFirst("span:containsOwn(Статус:) + span")?.ownText()?.trim { it <= ' ' } ?: ""
        val tt = TenderRfp(status, datePub, dateEnd, urlTend)
        tt.parsing()
    }

}