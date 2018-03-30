package enterit.parsers

import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlAnchor
import com.gargoylesoftware.htmlunit.html.HtmlButton
import com.gargoylesoftware.htmlunit.html.HtmlPage
import enterit.logger
import enterit.regExpTester
import enterit.tenders.TenderMiratorg
import java.util.logging.Level

class ParserMiratorg : Iparser {
    init {
        java.util.logging.Logger.getLogger("com.gargoylesoftware").level = Level.OFF
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog")
    }

    companion object WebCl {
        val webClient: WebClient = WebClient(BrowserVersion.CHROME)
    }

    val BaseUrl = "https://miratorg.ru/tenders/"
    override fun parser() {
        webClient.options.isThrowExceptionOnScriptError = false
        val page: HtmlPage = webClient.getPage(BaseUrl)
        try {
            parserPage(page)
        } catch (e: Exception) {
            logger("error in ${this::class.simpleName}.parserTender()", e.stackTrace, e)
        }
        webClient.close()
    }

    private fun parserPage(p: HtmlPage) {
        var page = p
        var button: HtmlButton?
        do {
            button = p.getFirstByXPath<HtmlButton>("//button[@class = 'press-list__more-button']")
            if (button != null) page = button.click()
        } while (button != null)
        val tenders = page.getByXPath<HtmlAnchor>("//a[@class = 'item']")
        tenders.forEach { t: HtmlAnchor ->
            try {
                parserTender(t)
            } catch (e: Exception) {
                logger("error in ${this::class.simpleName}.parserTender()", e.stackTrace, e)
            }
        }
    }

    private fun parserTender(a: HtmlAnchor) {
        val urlT = a.getAttribute("href") ?: ""
        val purNumT = a.getAttribute("data-url") ?: ""
        if (urlT == "" || purNumT == "") {
            logger("empty urlT or purNumT", a.asXml())
            return
        }
        val url = "https://miratorg.ru$urlT"
        val purNum = regExpTester("""id=(\d+)""", purNumT)
        val t = TenderMiratorg(purNum, url)
        t.parsing()
    }
}