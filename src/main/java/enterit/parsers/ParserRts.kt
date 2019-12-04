package enterit.parsers

import enterit.downloadFromUrl
import enterit.logger
import enterit.returnPriceEtpRf
import enterit.tenders.TenderRts
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class ParserRts : Iparser {
    companion object BaseTen {
        const val BaseT = "https://corp.rts-tender.ru"
    }

    private val baseUrl = "https://corp.rts-tender.ru/?fl=True&SearchForm.State=1&SearchForm.TenderRuleIds=4&SearchForm.MarketPlaceIds=5&SearchForm.CurrencyCode=undefined&&FilterData.PageCount=2&FilterData.PageIndex=1"
    private val maxPage = 5
    override fun parser() = (1..maxPage)
            .map { "https://corp.rts-tender.ru/?fl=True&SearchForm.State=1&SearchForm.TenderRuleIds=4&SearchForm.MarketPlaceIds=5&SearchForm.CurrencyCode=undefined&&FilterData.PageCount=$it&FilterData.PageIndex=1" }
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
            logger("Gets empty string ParserRts", url)
            return
        }
        val html = Jsoup.parse(stPage)
        val tenders = html.select("table.purchase-card")
        if (tenders.isEmpty()) {
            logger("Gets empty list tenders", url)
        }
        tenders.forEach<Element> { t ->
            try {
                val urlT = t.selectFirst("tbody > tr:eq(1) > td span.spoiler > a")?.attr("href")?.trim { it <= ' ' }
                        ?: ""
                val urlTend = "$BaseT$urlT"
                val purNum = t.selectFirst("tbody > tr:eq(0) li:contains(Номер на площадке) > p")?.ownText()?.trim { it <= ' ' }
                        ?: ""
                val plType = t.selectFirst("tbody > tr:eq(2) li.tag:eq(2) > p")?.ownText()?.trim { it <= ' ' }
                        ?: ""
                val applGuaranteeT = t.selectFirst("tbody > tr:eq(0) td.column-aside div:contains(Обеспечение заявки:) p strong")?.ownText()?.trim { it <= ' ' }
                        ?: ""
                val applGuarantee = returnPriceEtpRf(applGuaranteeT)
                val currency = t.selectFirst("tbody > tr:eq(0) td.column-aside div:contains(Обеспечение заявки:) p span")?.ownText()?.trim { it <= ' ' }
                        ?: ""
                val contrGuaranteeT = t.selectFirst("tbody > tr:eq(0) td.column-aside div:contains(Обеспечение контракта:) p strong")?.ownText()?.trim { it <= ' ' }
                        ?: ""
                val contrGuarantee = returnPriceEtpRf(contrGuaranteeT)
                var nmckT = t.selectFirst("tbody > tr:eq(0) td.column-aside div:contains(Начальная максимальная цена) p strong")?.ownText()?.trim { it <= ' ' }
                        ?: ""
                val nmck = returnPriceEtpRf(nmckT)
                val tt = TenderRts(urlTend, purNum, plType, applGuarantee, currency, contrGuarantee, nmck)
                tt.parsing()

            } catch (e: Exception) {
                logger("error in ParserRts.parserPage()", e.stackTrace, e)
            }
        }
    }
}