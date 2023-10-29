package enterit.parsers

import enterit.*
import enterit.tenders.TenderSiburNew
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class ParserSiburNew : Iparser{
    private val maxPage = 30

    companion object BaseTen {
        const val BaseT = "https://www.sibur.ru/procurement/buy/"
    }

    override fun parser() = (1..maxPage)
        .map { "https://www.sibur.ru/ru//procurement/buy/?lang=ru&PAGEN_1=$it" }
        .forEach {
            try {
                parserPage(it)
            } catch (e: Exception) {
                logger("Error in ParserSiburNew.parser function", e.stackTrace, e)
            }
        }

    private fun parserPage(url: String) {
        val stPage = downloadFromUrl(url, wt = 10000)
        if (stPage == "") {
            logger("Gets empty string ParserSibur", url)
            return
        }
        val html = Jsoup.parse(stPage)
        val tenders = html.select("tbody.table__table-body > tr")
        if (tenders.isEmpty()) {
            logger("Gets empty list tenders", url)
        }
        tenders.forEach<Element> { t ->
            try {
                val purNum = t.selectFirst("td:eq(0) a")?.ownText()?.trim { it <= ' ' }
                    ?: ""
                val purName = t.selectFirst("td:eq(1) > div:eq(0)")?.ownText()?.trim { it <= ' ' }
                    ?: ""
                val purObj = t.selectFirst("td:eq(2) > div:eq(0)")?.ownText()?.trim { it <= ' ' }
                    ?: ""
                val pwName = t.selectFirst("td:eq(3) > div:eq(0)")?.ownText()?.trim { it <= ' ' }
                    ?: ""
                val orgName = t.selectFirst("td:eq(6) > div:eq(0)")?.ownText()?.trim { it <= ' ' }
                    ?: ""
                val person = t.selectFirst("td:eq(8) > div:eq(0)")?.ownText()?.trim { it <= ' ' }
                    ?: ""
                val phone = t.selectFirst("td:eq(9) > div:eq(0)")?.ownText()?.trim { it <= ' ' }
                    ?: ""
                val email = t.selectFirst("td:eq(10) > div:eq(0)")?.ownText()?.trim { it <= ' ' }
                    ?: ""
                val status = t.selectFirst("td:eq(7) > div:eq(0)")?.ownText()?.trim { it <= ' ' }
                    ?: ""
                val datePubT = t.selectFirst("td:eq(4) > div:eq(0)")?.ownText()?.trim { it <= ' ' }
                    ?: ""
                val datePub = getDateFromFormat(datePubT, formatterOnlyDate)
                val dateEndT = t.selectFirst("td:eq(5) > div:eq(0)")?.ownText()?.trim { it <= ' ' }
                    ?: ""
                val dateEnd = getDateFromFormat(dateEndT, formatterGpn)
                val tt = TenderSiburNew(purNum, purName, purObj, pwName, datePub, dateEnd, orgName, status, person, phone, email)
                tt.parsing()
            } catch (e: Exception) {
                logger("error in ParserSiburNew.parserPage()", e.stackTrace, e)
            }
        }
    }
}