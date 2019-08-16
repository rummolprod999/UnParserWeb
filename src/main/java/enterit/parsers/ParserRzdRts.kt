package enterit.parsers

import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.*
import enterit.logger
import enterit.returnPriceEtpRf
import enterit.tenders.TenderRzdRts
import java.util.logging.Level

class ParserRzdRts : Iparser {
    companion object BaseTen {
        const val BaseT = "https://rzd.rts-tender.ru"
        val webClient: WebClient = WebClient(BrowserVersion.CHROME)
    }

    init {
        java.util.logging.Logger.getLogger("com.gargoylesoftware").level = Level.OFF
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog")
    }

    private val baseUrl = "https://rzd.rts-tender.ru/?fl=True&SearchForm.State=1&SearchForm.TenderRuleIds=2&SearchForm.TenderRuleIds=3&SearchForm.TenderRuleIds=4&SearchForm.CurrencyCode=undefined&FilterData.PageSize=100&FilterData.PageCount=1&FilterData.SortingField=DatePublished&FilterData.SortingDirection=Desc&&FilterData.PageIndex=1"
    private val timeout = 15_000L

    override fun parser() {
        webClient.options.isThrowExceptionOnScriptError = false
        webClient.waitForBackgroundJavaScript(timeout)
        try {

            val page: HtmlPage = webClient.getPage(baseUrl)
            webClient.waitForBackgroundJavaScript(timeout)
            parserPage(page)


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
        val urlTend = "$BaseT$urlT"
        val purNum = t.getFirstByXPath<HtmlParagraph>("./tbody/tr[1]//li[contains(., 'Номер на площадке')]//p").textContent.trim { it <= ' ' }
        val plType = t.getFirstByXPath<HtmlParagraph>("./tbody/tr[3]//li[contains(@class, 'tag')][3]/p").textContent.trim { it <= ' ' }
        val applGuaranteeT = t.getFirstByXPath<HtmlStrong>("./tbody/tr[1]//td[@class = 'column-aside']//div[contains(., 'Обеспечение заявки:')]//p//strong").textContent.trim { it <= ' ' }
        val applGuarantee = returnPriceEtpRf(applGuaranteeT)
        val currency = t.getFirstByXPath<HtmlSpan>("./tbody/tr[1]//td[@class = 'column-aside']//div[contains(., 'Обеспечение заявки:')]//p//span").textContent.trim { it <= ' ' }
        val contrGuaranteeT = t.getFirstByXPath<HtmlStrong>("./tbody/tr[1]//td[@class = 'column-aside']//div[contains(., 'Обеспечение контракта:')]//p//strong").textContent.trim { it <= ' ' }
        val contrGuarantee = returnPriceEtpRf(contrGuaranteeT)
        val status = t.getFirstByXPath<HtmlParagraph>(".//h6[. = 'Статус на площадке']/following-sibling::p").textContent.trim { it <= ' ' }
        val nmckT = t.getFirstByXPath<HtmlStrong>("./tbody/tr[1]//td[@class = 'column-aside']//div[contains(., 'Начальная максимальная цена')]//p//strong")?.textContent?.trim { it <= ' ' }
                ?: ""
        val nmck = returnPriceEtpRf(nmckT)
        val tt = TenderRzdRts(urlTend, purNum, plType, applGuarantee, currency, contrGuarantee, nmck, status)
        tt.parsing()
    }
}