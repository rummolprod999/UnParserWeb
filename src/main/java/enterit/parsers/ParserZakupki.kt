package enterit.parsers

import enterit.*
import enterit.tenders.TenderZakupki
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.util.*

class ParserZakupki : Iparser {
    val BaseUrl = "https://www.zakupki.ru"
    val arrayOffer = Array(50) { i -> "https://www.zakupki.ru/lot/list/offers/$i/?perpage=" }
    val arrayHot = Array(50) { i -> "https://www.zakupki.ru/lot/list/hot/$i/?perpage=" }
    override fun parser() {
        arrayOffer.forEach {
            try {
                parserPage(it)
            } catch (e: Exception) {
                logger("Error in ${this::class.simpleName}.parser() function", e.stackTrace, e)
            }
        }
        arrayHot.forEach {
            try {
                parserPage(it)
            } catch (e: Exception) {
                logger("Error in ${this::class.simpleName}.parser() function", e.stackTrace, e)
            }
        }
    }

    private fun parserPage(url: String) {
        val stPage = downloadFromUrl1251(url)
        if (stPage == "") {
            logger("Gets empty string ${this::class.simpleName}", url)
            return
        }
        val html = Jsoup.parse(stPage)
        val tenders = html.select("table.lk_isupply_table  tbody  tr")
        tenders.remove(tenders.last())
        if (tenders.isEmpty()) {
            //logger("Gets empty list tenders", url)
        }
        tenders.forEach<Element> { t ->
            try {
                parserTender(t)
            } catch (e: Exception) {
                logger("error in ${this::class.simpleName}.parserTender()", e.stackTrace, e)
            }
        }
    }

    private fun parserTender(el: Element) {
        val purNum = el.selectFirst("td:eq(0) a")?.ownText()?.trim { it <= ' ' } ?: ""
        if (purNum == "") run { logger("get empty purNum"); return }
        val urlT = el.selectFirst("td a")?.attr("href")?.trim { it <= ' ' }
                ?: ""
        if (urlT == "") run { logger("get empty urlT"); return }
        val urlTend = "$BaseUrl$urlT"
        val datePubTmp = el.selectFirst("td:eq(3)")?.ownText()?.trim { it <= ' ' }
                ?: ""
        val dateEndTmp = el.selectFirst("td:eq(4)")?.ownText()?.trim { it <= ' ' }
                ?: ""
        if (datePubTmp == "" || dateEndTmp == "") run { logger("get empty datePubTmp or dateEndTmp", urlTend); return }
        val datePub = getDateFromFormat(datePubTmp, formatterZakupkiDate)
        if (datePub == Date(0L)) run { logger("get empty datePub", urlTend); return }
        val dateEnd = getDateFromFormat(dateEndTmp, formatterZakupkiDateTime)
        if (dateEnd == Date(0L)) run { logger("get empty dateEnd", urlTend); return }
        val purObj = el.selectFirst("td:eq(1) a")?.ownText()?.trim { it <= ' ' }
                ?: ""
        val region = el.selectFirst("td:eq(2)")?.ownText()?.trim { it <= ' ' }
                ?: ""
        val tt = TenderZakupki(purNum, urlTend, purObj, region, datePub, dateEnd)
        tt.parsing()

    }
}