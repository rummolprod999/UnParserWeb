package enterit.parsers;

import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.*
import enterit.logger
import enterit.returnPriceEtpRf
import enterit.tenders.TenderRts
import java.util.logging.Level

class ParserRtsNew : Iparser {
    companion object BaseTen {
        const val BaseT = "https://corp.rts-tender.ru"
        val webClient: WebClient = WebClient(BrowserVersion.CHROME)
    }

    init {
        java.util.logging.Logger.getLogger("com.gargoylesoftware").level = Level.OFF
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog")
    }

    private val baseUrl = "https://corp.rts-tender.ru/?fl=True&SearchForm.State=1&SearchForm.TenderRuleIds=4&SearchForm.MarketPlaceIds=5&SearchForm.CurrencyCode=undefined&&FilterData.PageCount=2&FilterData.PageIndex=1"
    private val maxPage = 2
    override fun parser() {
        webClient.options.isThrowExceptionOnScriptError = false
        webClient.waitForBackgroundJavaScript(15000)
        try {
            (1..maxPage)
                    .map { "https://corp.rts-tender.ru/?fl=True&SearchForm.State=1&SearchForm.TenderRuleIds=4&SearchForm.MarketPlaceIds=5&SearchForm.CurrencyCode=undefined&&FilterData.PageCount=$it&FilterData.PageIndex=1" }
                    .forEach {
                        try {
                            val page: HtmlPage = webClient.getPage(it)
                            webClient.waitForBackgroundJavaScript(15000)
                            parserPage(page)
                        } catch (e: Exception) {
                            logger("Error in ParserRtsNew.parser function", e.stackTrace, e)
                        }
                    }
        } catch (e: Exception) {
            logger("Error in parser function", e.stackTrace, e)
        } finally {
            webClient.close()
        }
    }

    private fun parserPage(p: HtmlPage) {
        val tends: MutableList<HtmlTable> = p.getByXPath<HtmlTable>("//table[@class = 'purchase-card']")
        for (i in tends) {
            try {
                parserTender(i)
            } catch (e: Exception) {
                logger("Error in parserPage function", e.stackTrace, e)
            }
        }
    }

    private fun parserTender(t: HtmlTable) {
        val urlT = t.getFirstByXPath<HtmlAnchor>("./tbody/tr[2]/td//span[@class = 'spoiler']/a").getAttribute("href")
        val urlTend = "$urlT"
        val purNum = t.getFirstByXPath<HtmlParagraph>("./tbody/tr[1]//li[contains(., 'Номер на площадке')]//p").textContent.trim { it <= ' ' }
        val plType = t.getFirstByXPath<HtmlParagraph>("./tbody/tr[3]//li[contains(@class, 'tag')][3]/p").textContent.trim { it <= ' ' }
        val applGuaranteeT = t.getFirstByXPath<HtmlStrong>("./tbody/tr[1]//td[@class = 'column-aside']//div[contains(., 'Обеспечение заявки:')]//p//strong").textContent.trim { it <= ' ' }
        val applGuarantee = returnPriceEtpRf(applGuaranteeT)
        val currency = t.getFirstByXPath<HtmlSpan>(".//h5[contains(., 'Начальная максимальная цена')]/following-sibling::p/span").textContent.trim { it <= ' ' }
        val contrGuaranteeT = t.getFirstByXPath<HtmlStrong>("./tbody/tr[1]//td[@class = 'column-aside']//div[contains(., 'Обеспечение контракта:')]//p//strong").textContent.trim { it <= ' ' }
        val contrGuarantee = returnPriceEtpRf(contrGuaranteeT)
        val nmckT = t.getFirstByXPath<HtmlStrong>(".//h5[contains(., 'Начальная максимальная цена')]/following-sibling::p/strong")?.textContent?.trim { it <= ' ' }
                ?: ""
        val nmck = returnPriceEtpRf(nmckT)
        val tt = TenderRts(urlTend, purNum, plType, applGuarantee, currency, contrGuarantee, nmck)
        tt.parsing()
    }
}
