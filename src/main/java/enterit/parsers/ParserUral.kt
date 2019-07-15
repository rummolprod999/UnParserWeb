package enterit.parsers

import enterit.*
import enterit.tenders.TenderUral
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class ParserUral : Iparser {
    val BaseUrl = "https://www.uralkali.com"
    private val baseUrl = "https://www.uralkali.com/ru/tenders/?PAGEN_1=8"
    override fun parser() {
        try {
            parserPage(baseUrl)
        } catch (e: Exception) {
            logger("Error in ${this::class.simpleName}.parser function", e.stackTrace, e)
        }
    }

    private fun parserPage(url: String) {
        val stPage = downloadFromUrl1251(url)
        if (stPage == "") {
            logger("Gets empty string ${this::class.simpleName}", url)
            return
        }
        val html = Jsoup.parse(stPage)
        val tenders = html.select("table[class *= competitive_tendering with_sections]  tbody  tr")
        if (tenders.isEmpty()) {
            logger("Gets empty list tenders", url)
        }
        tenders.forEach<Element> { t ->
            try {
                val urlT = t.selectFirst("td a")?.attr("href")?.trim { it <= ' ' }
                        ?: ""
                val urlTend = "$BaseUrl$urlT"
                val datePubTmp = t.selectFirst("td.date_start")?.ownText()?.trim { it <= ' ' }
                        ?: ""
                val dateEndTmp = t.selectFirst("td.date_end")?.ownText()?.trim { it <= ' ' }
                        ?: ""
                val datePub = getDateFromFormat(datePubTmp, formatterOnlyDate)
                val dateEnd = getDateFromFormat(dateEndTmp, formatterOnlyDate)
                val purObj = t.selectFirst("td a")?.ownText()?.trim { it <= ' ' }
                        ?: ""
                val purNum = regExpTester("""/ru/tenders/(\d+)\.html""", urlTend)
                val tt = TenderUral(purNum, purObj, datePub, dateEnd, urlTend)
                tt.parsing()
            } catch (e: Exception) {
                logger("error in ${this::class.simpleName}.parserTender()", e.stackTrace, e)
            }
        }
    }
}