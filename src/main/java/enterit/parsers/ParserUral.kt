package enterit.parsers

import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlAnchor
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.gargoylesoftware.htmlunit.html.HtmlTableCell
import com.gargoylesoftware.htmlunit.html.HtmlTableRow
import enterit.formatterOnlyDate
import enterit.getDateFromFormat
import enterit.logger
import enterit.regExpTester
import enterit.tenders.TenderUral
import java.util.logging.Level

class ParserUral : Iparser {
    val BaseUrl = "https://www.uralkali.com"
    private val baseUrl = "https://www.uralkali.com/ru/tenders/?PAGEN_1=8"
    val webClient: WebClient = WebClient(BrowserVersion.CHROME)

    init {
        java.util.logging.Logger.getLogger("com.gargoylesoftware").level = Level.OFF
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog")
    }

    override fun parser() {
        try {
            webClient.options.isThrowExceptionOnScriptError = false
            webClient.waitForBackgroundJavaScript(15000)
            parserPage(baseUrl)
            (2..5).forEach({
                val url = "https://www.uralkali.com/ru/tenders/?PAGEN_1=8&PAGEN_5=$it";
                parserPage(url)
            })
        } catch (e: Exception) {
            logger("Error in ${this::class.simpleName}.parser function", e.stackTrace, e)
        }
    }

    private fun parserPage(url: String) {

        val page: HtmlPage = webClient.getPage(url)
        webClient.waitForBackgroundJavaScript(15000)

        val tenders =
            page.getByXPath<HtmlTableRow>("//table[contains(concat(\" \",normalize-space(@class),\" \"),\" competitive_tendering \")][contains(concat(\" \",normalize-space(@class),\" \"),\" with_sections \")]//tbody//tr")
        if (tenders.isEmpty()) {
            logger("Gets empty list tenders", url)
        }
        tenders.forEach { t ->
            try {
                val urlT = t.getFirstByXPath<HtmlAnchor>(".//td/a")?.getAttribute("href")?.trim { it <= ' ' }
                    ?: ""
                val urlTend = "$BaseUrl$urlT"
                val datePubTmp =
                    t.getFirstByXPath<HtmlTableCell>(".//td[contains(@class, 'date_start')]")?.textContent?.trim { it <= ' ' }
                        ?: ""
                val dateEndTmp =
                    t.getFirstByXPath<HtmlTableCell>(".//td[contains(@class, 'date_end')]")?.textContent?.trim { it <= ' ' }
                        ?: ""
                val datePub = getDateFromFormat(datePubTmp, formatterOnlyDate)
                val dateEnd = getDateFromFormat(dateEndTmp, formatterOnlyDate)
                val purObj = t.getFirstByXPath<HtmlAnchor>(".//td/a")?.textContent?.trim { it <= ' ' }
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