package enterit.parsers

import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.gargoylesoftware.htmlunit.html.HtmlSpan
import com.gargoylesoftware.htmlunit.html.HtmlTableRow
import enterit.formatterEtpRf
import enterit.formatterEtpRfN
import enterit.getDateFromFormat
import enterit.logger
import enterit.tenders.TenderEtpRf
import java.util.logging.Level
import java.util.regex.Matcher
import java.util.regex.Pattern

const val PageNumEtpRf = 50

class ParserEtpRf : Iparser {
    val BaseUrl = "http://etprf.ru"
    val urlEtprf = listOf("http://etprf.ru/NotificationEX", "http://etprf.ru/BRNotification")

    init {
        java.util.logging.Logger.getLogger("com.gargoylesoftware").level = Level.OFF
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog")
    }

    override fun parser() {
        val webClient = WebClient(BrowserVersion.CHROME)
        webClient.options.isThrowExceptionOnScriptError = false
        urlEtprf.forEach { i -> parserE(webClient, i) }
        webClient.close()
    }

    private fun parserE(webClient: WebClient, pg: String) {
        val page: HtmlPage = webClient.getPage(pg)
        if (pg == "http://etprf.ru/BRNotification") {
            try {
                parserPage(page)
            } catch (e: Exception) {
                logger("Error in parserE function", e.stackTrace, e)
            }
        } else if (pg == "http://etprf.ru/NotificationEX") {
            try {
                parserPageN(page)
            } catch (e: Exception) {
                logger("Error in parserE function", e.stackTrace, e)
            }
        }
        for (i in 1..PageNumEtpRf) {
            val button = page.getByXPath<HtmlSpan>("//span[@class = 'ui-button-text' and . = 'Вперед']")
            val b = button[0] as HtmlSpan
            val y: HtmlPage = b.click()
            if (pg == "http://etprf.ru/BRNotification") {
                try {
                    parserPage(y)
                } catch (e: Exception) {
                    logger("Error in parserPage function", e.stackTrace, e)
                }
            } else if (pg == "http://etprf.ru/NotificationEX") {
                try {
                    parserPageN(y)
                } catch (e: Exception) {
                    logger("Error in parserPageN function", e.stackTrace, e)
                }
            }

        }
    }

    private fun parserPage(p: HtmlPage) {
        val tends: MutableList<HtmlTableRow> = p.getByXPath<HtmlTableRow>("//table[@class = 'reporttable']/tbody/tr[@id]")
        for (i in tends) parserTender(i)
    }

    private fun parserPageN(p: HtmlPage) {
        val tends: MutableList<HtmlTableRow> = p.getByXPath<HtmlTableRow>("//table[@class = 'reporttable']/tbody/tr[@id]")
        for (i in tends) parserTenderN(i)
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
            val datePub = getDateFromFormat(datePubTmp, formatterEtpRf)
            val dateEnd = getDateFromFormat(dateEndTmp, formatterEtpRf)
            val urlT = t.getCell(11).getElementsByTagName("a")[0].getAttribute("href")
            val url = "$BaseUrl$urlT"
            val tt = TenderEtpRf(status, entNum, purNum, purObj, nmck, placingWay, datePub, dateEnd, url)
            tt.parsing()
        } catch (e: Exception) {
            logger("error in ParserEtpRf.parserTender()", e.stackTrace, e)
        }

    }

    private fun parserTenderN(t: HtmlTableRow) {
        try {
            var status = t.getCell(8).textContent.trim { it <= ' ' }
            if (status.contains("ЕИС")) return
            val statusT = t.getCell(7).textContent.trim { it <= ' ' }
            if (statusT != "") status = statusT
            val entNum = t.getCell(0).textContent.trim { it <= ' ' }
            var purNum = t.getCell(1).textContent.trim { it <= ' ' }
            val pattern: Pattern = Pattern.compile("\\s+")
            val matcher: Matcher = pattern.matcher(purNum)
            purNum = matcher.replaceAll("")
            val purObj = t.getCell(2).textContent.trim { it <= ' ' }
            val nmck = t.getCell(3).textContent.trim { it <= ' ' }
            val placingWay = ""
            val datePubTmp = t.getCell(5).textContent.trim { it <= ' ' }
            val dateEndTmp = t.getCell(6).textContent.trim { it <= ' ' }
            val datePub = getDateFromFormat(datePubTmp, formatterEtpRfN)
            val dateEnd = getDateFromFormat(dateEndTmp, formatterEtpRfN)
            val urlT = t.getCell(9).getElementsByTagName("a")[0].getAttribute("href")
            val url = "$BaseUrl$urlT"
            val tt = TenderEtpRf(status, entNum, purNum, purObj, nmck, placingWay, datePub, dateEnd, url)
            tt.parsing()
        } catch (e: Exception) {
            logger("error in ParserEtpRf.parserTenderN()", e.stackTrace, e)
        }
    }

}