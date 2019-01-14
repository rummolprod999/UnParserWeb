package enterit.parsers

import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlAnchor
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.gargoylesoftware.htmlunit.html.HtmlTableRow
import enterit.formatterZakupkiDate
import enterit.formatterZakupkiDateTime
import enterit.getDateFromFormat
import enterit.logger
import enterit.tenders.TenderZakupki
import org.jsoup.nodes.Element
import java.util.*
import java.util.logging.Level

class ParserZakupki : Iparser {
    private val _baseUrl = "https://www.zakupki.ru"

    companion object WebCl {
        val webClient: WebClient = WebClient(BrowserVersion.FIREFOX_52)
        const val numPage = 50
    }

    init {
        java.util.logging.Logger.getLogger("com.gargoylesoftware").level = Level.OFF
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog")
    }

    override fun parser() {
        webClient.waitForBackgroundJavaScript(10000)
        val page: HtmlPage = webClient.getPage(_baseUrl)
        Thread.sleep(5000)
        try {
            try {
                parserPage(page)
            } catch (e: Exception) {
                logger("Error in parser function", e.stackTrace, e)
            }
            for (i in 1..numPage) {
                val button = page.getByXPath<HtmlAnchor>("//a[. = 'Следущая']")
                if (!button.isEmpty()) {
                    val b = button[0] as HtmlAnchor
                    val y: HtmlPage = b.click()
                    y.webClient.waitForBackgroundJavaScript(5000)
                    Thread.sleep(5000)
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

    private fun parserPage(page: HtmlPage) {
        //println(page.asXml())
        val tends: MutableList<HtmlTableRow> = page.getByXPath<HtmlTableRow>("//table[@class = 'lk_isupply_table']/tbody[2]/tr")
        for (i in tends) {
            try {
                parserTender(i)
            } catch (e: Exception) {
                logger("Error in parserPage function", e.stackTrace, e)
            }
        }
    }

    private fun parserTender(t: HtmlTableRow) {
        val purNum = t.getCell(0).getElementsByTagName("a")[0].textContent.trim { it <= ' ' }
        if (purNum == "") {
            run { logger("get empty purNum"); return }
        }
        var url = ""
        val urlT = t.getCell(0).getElementsByTagName("a")
        if (!urlT.isEmpty()) {
            url = "$_baseUrl${urlT[0].getAttribute("href")}"
        }
        if (url == "") run { logger("get empty url"); return }
        val purObj = t.getCell(1).getElementsByTagName("a")[0].textContent.trim { it <= ' ' }
        val datePubTmp = t.getCell(3).textContent.trim { it <= ' ' }
        val dateEndTmp0 = t.getCell(4).firstChild.textContent.trim { it <= ' ' }
        val dateEndTmp1 = t.getCell(4).lastChild.textContent.trim { it <= ' ' }
        val dateEndTmp = "$dateEndTmp0 $dateEndTmp1"
        if (datePubTmp == "" || dateEndTmp == "") run { logger("get empty datePubTmp or dateEndTmp", url); return }
        val datePub = getDateFromFormat(datePubTmp, formatterZakupkiDate)
        if (datePub == Date(0L)) run { logger("get empty datePub", url); return }
        val dateEnd = getDateFromFormat(dateEndTmp, formatterZakupkiDateTime)
        if (dateEnd == Date(0L)) run { logger("get empty dateEnd", url); return }
        val region = t.getCell(2).textContent.trim { it <= ' ' }
        val tt = TenderZakupki(purNum, url, purObj, region, datePub, dateEnd)
        tt.parsing()
    }

    private fun parserTender(el: Element) {
        if (el.text().contains("Дата публикации")) return
        val purNum = el.selectFirst("td:eq(0) a")?.ownText()?.trim { it <= ' ' } ?: ""
        if (purNum == "") {
            run { logger("get empty purNum"); return }
        }
        val urlT = el.selectFirst("td a")?.attr("href")?.trim { it <= ' ' }
                ?: ""
        if (urlT == "") run { logger("get empty urlT"); return }
        val urlTend = "$_baseUrl$urlT"
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