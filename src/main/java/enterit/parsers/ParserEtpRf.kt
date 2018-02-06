package enterit.parsers

import com.gargoylesoftware.htmlunit.html.*
import com.gargoylesoftware.htmlunit.*
import enterit.getDateEtpRf
import enterit.logger
import enterit.tenders.TenderEtpRf
import java.util.logging.Level
import java.util.regex.Matcher
import java.util.regex.Pattern

const val PageNumEtpRf = 30

class ParserEtpRf : Iparser {
    val BaseUrl = "http://etprf.ru"
    init {
        java.util.logging.Logger.getLogger("com.gargoylesoftware").level = Level.OFF
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog")
    }

    override fun parser() {
        val webClient = WebClient(BrowserVersion.CHROME)
        val page: HtmlPage = webClient.getPage("http://etprf.ru/BRNotification")
        try {
            parserPage(page)
        } catch (e: Exception) {
            logger("Error in parserPage function", e.stackTrace, e)
        }
        for (i in 1..PageNumEtpRf) {
            val button = page.getByXPath<HtmlSpan>("//span[@class = 'ui-button-text' and . = 'Вперед']")
            val b = button[0] as HtmlSpan
            val y: HtmlPage = b.click()
            try {
                parserPage(y)
            } catch (e: Exception) {
                logger("Error in parserPage function", e.stackTrace, e)
            }
        }
        webClient.close()
    }

    private fun parserPage(p: HtmlPage) {
        val tends: MutableList<HtmlTableRow> = p.getByXPath<HtmlTableRow>("//table[@class = 'reporttable']/tbody/tr[@id]")
        for (i in tends) {
            parserTender(i)
        }
    }

    private fun parserTender(t: HtmlTableRow) {
        try {
            val status = t.getCell(10).textContent.trim { it <= ' ' }
            val entNum = t.getCell(0).textContent.trim { it <= ' ' }
            var purNum = t.getCell(1).textContent.trim { it <= ' ' }
            val pattern: Pattern = Pattern.compile("\\s+")
            val matcher: Matcher = pattern.matcher(purNum)
            purNum = matcher.replaceAll("")
            val purObj = t.getCell(3).textContent.trim { it <= ' ' }
            val nmck = t.getCell(4).textContent.trim { it <= ' ' }
            val placingWay = t.getCell(6).textContent.trim { it <= ' ' }
            val datePubTmp = t.getCell(7).textContent.trim { it <= ' ' }
            val dateEndTmp = t.getCell(8).textContent.trim { it <= ' ' }
            val datePub = getDateEtpRf(datePubTmp)
            val dateEnd = getDateEtpRf(dateEndTmp)
            val urlT = t.getCell(11).getElementsByTagName("a")[0].getAttribute("href")
            val url = "$BaseUrl$urlT"
            val tt = TenderEtpRf(status, entNum, purNum, purObj, nmck, placingWay, datePub, dateEnd, url)
            tt.parsing()
        } catch (e: Exception) {
            logger("error in ParserEtpRf.parserTender()", e.stackTrace, e)
        }

    }

}