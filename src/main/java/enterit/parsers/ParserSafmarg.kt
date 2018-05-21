package enterit.parsers

import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.gargoylesoftware.htmlunit.html.HtmlTableRow
import enterit.logger
import java.util.logging.Level

class ParserSafmarg : Iparser {
    init {
        java.util.logging.Logger.getLogger("com.gargoylesoftware").level = Level.OFF
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog")
    }

    companion object WebCl {
        val webClient: WebClient = WebClient(BrowserVersion.CHROME)
        const val timeoutB = 20000L
        const val BaseUrl = "http://тендеры.талан.рф/Tenders#page="
        const val CountPage = 5
    }

    override fun parser() {
        webClient.options.isThrowExceptionOnScriptError = false

        webClient.waitForBackgroundJavaScript(timeoutB)
        try {
            (1..CountPage).map {
                val url = "$BaseUrl$it"
                println(url)
                val page: HtmlPage = webClient.getPage(url)
                getListTenders(page)
            }

        } catch (e: Exception) {
            logger("error in ${this::class.simpleName}.getListTenders()", e.stackTrace, e)
            throw e
        } finally {
            webClient.close()
        }
    }

    private fun getListTenders(p: HtmlPage) {
        var p = p
        val tenders: MutableList<HtmlTableRow> = p.getByXPath<HtmlTableRow>("//table[@id = 'grid_TenderGridViewModel']/tbody/tr")
        tenders.forEach {
            try {
                parserListTenders(it)
            } catch (e: Exception) {
                logger("error in ${this::class.simpleName}.parserListTenders()", e.stackTrace, e, p.asXml())
            }
        }
    }

    private fun parserListTenders(t: HtmlTableRow) {
        val purNum = t.getCell(0)?.textContent?.trim { it <= ' ' } ?: ""
        ?: ""
        println(purNum)
    }
}