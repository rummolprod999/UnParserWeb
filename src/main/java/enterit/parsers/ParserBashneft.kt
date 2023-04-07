package enterit.parsers

import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlAnchor
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.gargoylesoftware.htmlunit.html.HtmlTableRow
import enterit.formatter
import enterit.getDateFromString
import enterit.logger
import enterit.tenders.TenderBashneft
import java.util.*
import java.util.logging.Level

class ParserBashneft : Iparser {
    init {
        java.util.logging.Logger.getLogger("com.gargoylesoftware").level = Level.OFF
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog")
    }

    companion object WebCl {
        val webClient: WebClient = WebClient(BrowserVersion.FIREFOX)
        const val timeoutB = 20000L
        const val BaseUrl = "http://etp.bashneft.ru/"
        const val CountPage = 2
    }

    private val listTenders: MutableList<HtmlTableRow> = mutableListOf()

    override fun parser() {
        webClient.options.isThrowExceptionOnScriptError = false
        val page: HtmlPage = webClient.getPage(BaseUrl)
        /*val cm = CookieManager()
        webClient.cookieManager = cm*/
        webClient.waitForBackgroundJavaScript(timeoutB)
        try {
            getListTenders(page)
        } catch (e: Exception) {
            logger("error in ${this::class.simpleName}.getListTenders()", e.stackTrace, e)
            throw e
        } finally {
            webClient.close()
        }
    }

    private fun getListTenders(p: HtmlPage) {
        addTenderToList(p)
        var p = p
        (1..CountPage).map {
            val button =
                p.getFirstByXPath<HtmlAnchor>("//div[@class = 'dxgvPagerBottomPanel_BashneftTheme']//a[contains(@class, 'dxp-button dxp-bi')]")
            button?.let {
                p = it.click()
                p.webClient.waitForBackgroundJavaScript(5000)
                addTenderToList(p)

            }
        }
        listTenders.forEach {
            try {
                parserListTenders(it)
            } catch (e: Exception) {
                logger("error in ${this::class.simpleName}.parserListTenders()", e.stackTrace, e, p.asXml())
            }
        }

    }

    private fun parserListTenders(t: HtmlTableRow) {
        val purNum = t.getCell(0)?.textContent?.trim { it <= ' ' } ?: ""
        val purObjInfo = t.getCell(1)?.textContent?.trim { it <= ' ' } ?: ""
        if (purObjInfo == "") {
            //logger("can not find purObjInfo in tender")
            return
        }
        if (purNum == "") {
            logger("can not find purNum in tender")
            return
        }
        val urlT = t.getCell(0).getElementsByTagName("a")[0].getAttribute("href")
        val url = "$BaseUrl$urlT"
        val placingWayName = t.getCell(5)?.textContent?.trim { it <= ' ' } ?: ""
        val status = t.getCell(6)?.textContent?.trim { it <= ' ' } ?: ""
        val datePubTmp = t.getCell(3).textContent.trim { it <= ' ' }
        val dateEndTmp = t.getCell(4).textContent.trim { it <= ' ' }
        val datePub = datePubTmp.getDateFromString(formatter)
        val dateEnd = dateEndTmp.getDateFromString(formatter)
        if (datePub == Date(0L) || dateEnd == Date(0L)) {
            logger("can not find pubDate or endDate on page", url)
            return
        }
        val tt = TenderBashneft(status, purNum, url, purObjInfo, placingWayName, datePub, dateEnd)
        try {
            tt.parsing()
        } catch (e: Exception) {
            logger("error in TenderBashneft.parsing()", e.stackTrace, e, url)
        }
    }

    private fun addTenderToList(p: HtmlPage) {
        val tenders: MutableList<HtmlTableRow> =
            p.getByXPath<HtmlTableRow>("//div[@class = 'dxtc-content']//table[@class = 'dxgvTable_BashneftTheme']//tr[contains(@class, 'dxgvDataRow_BashneftTheme')]")
        listTenders.addAll(tenders)

    }
}