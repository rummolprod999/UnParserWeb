package enterit.parsers

import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlButton
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.gargoylesoftware.htmlunit.html.HtmlTableRow
import enterit.formatterGpn
import enterit.getDateFromFormat
import enterit.logger
import enterit.returnPriceEtpRf
import enterit.tenders.TenderTat
import java.util.logging.Level

const val PageNumTat = 10

class ParserTat : Iparser {
    companion object WebCl {
        val webClient: WebClient = WebClient(BrowserVersion.CHROME)
    }

    val Url = "https://etp.tatneft.ru/pls/tzp/f?p=220:562:14617926409515::::P562_OPEN_MODE,GLB_NAV_ROOT_ID,GLB_NAV_ID:,12920020,12920020"
    val BaseUrl = "https://etp.tatneft.ru/pls/tzp/"

    init {
        java.util.logging.Logger.getLogger("com.gargoylesoftware").level = Level.OFF
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog")
    }

    override fun parser() {

        webClient.waitForBackgroundJavaScript(5000)
        val page: HtmlPage = webClient.getPage(Url)
        try {
            try {
                parserPage(page)
            } catch (e: Exception) {
                logger("Error in parser function", e.stackTrace, e)
            }
            for (i in 1..PageNumTat) {
                val button = page.getByXPath<HtmlButton>("//button[@aria-label = '>']")
                if (!button.isEmpty()) {
                    val b = button[0] as HtmlButton
                    val y: HtmlPage = b.click()
                    y.webClient.waitForBackgroundJavaScript(5000)
                    try {
                        parserPage(y)
                    } catch (e: Exception) {
                        logger("Error in parser function", e.stackTrace, e)
                    }
                }
            }
        } catch (e: Exception) {
            logger("Error in parser function", e.stackTrace, e)
        } finally {
            webClient.close()
        }

    }

    private fun parserPage(p: HtmlPage) {
        val tends: MutableList<HtmlTableRow> = p.getByXPath<HtmlTableRow>("//div[@class = 'a-IRR-tableContainer']/table/tbody/tr[position() > 1]")
        for (i in tends) {
            try {
                parserTender(i)
            } catch (e: Exception) {
                logger("Error in parserPage function", e.stackTrace, e)
            }
        }
    }

    private fun parserTender(t: HtmlTableRow) {
        val status = t.getCell(3).textContent.trim { it <= ' ' }
        val purNum = t.getCell(1).textContent.trim { it <= ' ' }
        val purObj = t.getCell(2).textContent.trim { it <= ' ' }
        var url = ""
        val urlT = t.getCell(2).getElementsByTagName("a")
        if (!urlT.isEmpty()) {
            url = "$BaseUrl${urlT[0].getAttribute("href")}"
        }
        val fullNameOrg = t.getCell(4).textContent.trim { it <= ' ' }
        val nMckT = t.getCell(5).textContent.trim { it <= ' ' }
        val currency = t.getCell(6).textContent.trim { it <= ' ' }
        val nMck = returnPriceEtpRf(nMckT)
        val datePubTmp = t.getCell(7).textContent.trim { it <= ' ' }
        val dateEndTmp = t.getCell(9).textContent.trim { it <= ' ' }
        val datePub = getDateFromFormat(datePubTmp, formatterGpn)
        val dateEnd = getDateFromFormat(dateEndTmp, formatterGpn)
        val wb = WebClient(BrowserVersion.CHROME)
        val tt = TenderTat(status, purNum, purObj, nMck, datePub, dateEnd, url, fullNameOrg, currency, wb)
        try {
            tt.parsing()
        } catch (e: Exception) {
            logger("Error in parserTender function", e.stackTrace, e)
        } finally {
            wb.close()
        }

    }
}