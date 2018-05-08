package enterit.parsers

import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.gargoylesoftware.htmlunit.html.HtmlTableHeaderCell
import enterit.logger

class ParserButb : Iparser {
    init {
        //java.util.logging.Logger.getLogger("com.gargoylesoftware").level = Level.OFF
        // System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog")
    }

    companion object WebCl {
        val webClient: WebClient = WebClient(BrowserVersion.CHROME)
        const val timeoutB = 20000L
        const val BaseUrl = "http://zakupki.butb.by/auctions/reestrauctions.html"
        const val CountPage = 2
    }

    override fun parser() {
        webClient.options.isThrowExceptionOnScriptError = false
        val page: HtmlPage = webClient.getPage(BaseUrl)
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
        var p = p
        val button = p.getFirstByXPath<HtmlTableHeaderCell>("//table[contains(@id, 'auctionList')]//a[contains(., 'Дата публикации')]/..")
        println(button)
        button?.let {
            p = it.click()
            p.webClient.waitForBackgroundJavaScript(5000)
            p = it.click()
        }
        println(p.asText())
        println(p.asXml())
    }
}