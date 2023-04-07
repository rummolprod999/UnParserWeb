package enterit.parsers

import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.*
import enterit.CountStg
import enterit.PassStg
import enterit.UserStg
import enterit.logger
import enterit.tenders.TenderStg
import java.util.logging.Level

class ParserStg : Iparser {
    init {
        java.util.logging.Logger.getLogger("com.gargoylesoftware").level = Level.OFF
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog")
    }

    companion object WebCl {
        val webClient: WebClient = WebClient(BrowserVersion.FIREFOX)
        const val timeoutB = 20000L
        const val BaseUrl = "https://tender.stg.ru/main/sso/Login.aspx"
    }

    private val listTenders: MutableList<HtmlTableRow> = mutableListOf()
    override fun parser() {
        webClient.options.isThrowExceptionOnScriptError = false
        val page: HtmlPage = webClient.getPage(BaseUrl)
        /*val cm = CookieManager()
        webClient.cookieManager = cm*/
        webClient.waitForBackgroundJavaScript(timeoutB)
        try {
            loggingInSys(page)
        } catch (e: Exception) {
            logger("error in ${this::class.simpleName}.loggingInSys()", e.stackTrace, e)
            throw e
        } finally {
            webClient.close()
        }

    }

    private fun loggingInSys(p: HtmlPage) {
        val logForm = p.getFirstByXPath<HtmlAnchor>("//a[@id='enter']")
        if (logForm != null) {
            val l = logForm.click<HtmlPage>()
            val usr = l.getFirstByXPath<HtmlInput>("//input[@id='MainContent_txtUserName']")
            val pass = l.getFirstByXPath<HtmlInput>("//input[@id='MainContent_txtUserPassword']")
            val inp = l.getFirstByXPath<HtmlInput>("//input[@id='MainContent_btnLogin']")
            usr.valueAttribute = UserStg
            pass.valueAttribute = PassStg
            val pg = inp.click<HtmlPage>()
            pg.webClient.waitForBackgroundJavaScript(timeoutB)
            if (pg != null) {
                try {
                    parserList(pg)
                } catch (e: Exception) {
                    logger("Error in parserList function", e.stackTrace, e)
                }
                for (i in 1..CountStg) {
                    val button =
                        pg.getFirstByXPath<HtmlSpan>("//a[contains(@class, 'k-pager-nav') and not(contains(@class, 'k-state-disabled'))]/span[contains(@class, 'k-i-arrow-e')]")
                    if (button is HtmlSpan) {
                        val y: HtmlPage = button.click()
                        y.webClient.waitForBackgroundJavaScript(timeoutB)
                        try {
                            parserList(y)
                        } catch (e: Exception) {
                            logger("Error in parserList function", e.stackTrace, e)
                        }
                    } else {
                        logger("can not find span next on site")
                    }
                }
            } else {
                logger("can not autorization on site")
            }
        } else {
            logger("can not find login form on page")
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
        val status = t.getCell(3)?.textContent?.trim { it <= ' ' } ?: ""
        val placingWayName = t.getCell(4)?.textContent?.trim { it <= ' ' } ?: ""
        val orgFullName = t.getCell(5)?.textContent?.trim { it <= ' ' } ?: ""
        val regionName = t.getCell(8)?.textContent?.trim { it <= ' ' } ?: ""
        val tt = TenderStg(status, purNum, urlT, purObjInfo, orgFullName, placingWayName, regionName)
        try {
            tt.parsing()
        } catch (e: Exception) {
            logger("error in TenderStg.parsing()", e.stackTrace, e)
        }
    }

    private fun parserList(p: HtmlPage) {
        val tenders =
            p.getByXPath<HtmlTableRow>("//div[@class = 'last']//div[@class = 'k-grid-content']//tbody[@role = 'rowgroup']/tr")
        if (tenders is List<HtmlTableRow>) {
            tenders.forEach {
                try {
                    parserT(it)
                } catch (e: Exception) {
                    logger("error in ${this::class.simpleName}.parserList()", e.stackTrace, e)
                }
            }
        } else {
            logger("can not find tenders on page")
        }
        //logger(p.asXml())
    }

    private fun parserT(t: HtmlTableRow) {
        listTenders.add(t)
    }
}