package enterit.parsers

import com.gargoylesoftware.htmlunit.html.*
import com.gargoylesoftware.htmlunit.*
import enterit.getDateEtpRf
import enterit.logger
import java.util.logging.Level

const val PageNumEtpRf = 10

class ParserEtpRf : Iparser {
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
        val status = t.getCell(10).textContent.trim { it <= ' ' }
        val entNum = t.getCell(0).textContent.trim { it <= ' ' }
        val purNum = t.getCell(1).textContent.trim { it <= ' ' }
        val purObj =  t.getCell(3).textContent.trim { it <= ' ' }
        val nmck =  t.getCell(4).textContent.trim { it <= ' ' }
        val placingWay = t.getCell(6).textContent.trim { it <= ' ' }
        val datePubTmp = t.getCell(7).textContent.trim { it <= ' ' }
        val dateEndTmp = t.getCell(8).textContent.trim { it <= ' ' }
        val dateEnd = getDateEtpRf(dateEndTmp)
        println(dateEndTmp)
        println(dateEnd)
        println()
    }

}