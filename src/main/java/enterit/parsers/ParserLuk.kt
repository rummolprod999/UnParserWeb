package enterit.parsers

import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlAnchor
import com.gargoylesoftware.htmlunit.html.HtmlDivision
import com.gargoylesoftware.htmlunit.html.HtmlPage
import enterit.logger
import enterit.tenders.TenderLuk
import java.util.logging.Level

class ParserLuk : Iparser {


    init {
        java.util.logging.Logger.getLogger("com.gargoylesoftware").level = Level.OFF
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog")
    }

    override fun parser() {
        val webClient = WebClient(BrowserVersion.CHROME)
        val page: HtmlPage = webClient.getPage("http://www.lukoil.ru/Company/Tendersandauctions/Tenders")
        page.webClient.waitForBackgroundJavaScript(5000)
        try {
            parserPage(page)
        } catch (e: Exception) {
            logger("Error in parserPage function", e.stackTrace, e)
        }
    }

    private fun parserPage(p: HtmlPage) {
        val page = Companion.getTendersPage(p)
        val ten = page.getByXPath<HtmlDivision>("//div[@class = 'panel-default panel-collapsible panel-tender']")
        ten.forEach { t: HtmlDivision ->
            try {
                val luk = TenderLuk(t)
                luk.parser()
            } catch (e: Exception) {
                logger("Error in parserPage function", e.stackTrace, e)
            }
        }
    }

    companion object {
        private fun getTendersPage(p: HtmlPage): HtmlPage {
            var p = p
            try {
                var button = p.getByXPath<HtmlAnchor>("//a[@class = 'button button-loadmore' and . = 'Загрузить больше' and @style = '']")

                while (button.size != 0) {
                    p = button[0].click()
                    p.webClient.waitForBackgroundJavaScript(5000)
                    button = p.getByXPath<HtmlAnchor>("//a[@class = 'button button-loadmore' and . = 'Загрузить больше' and @style = '']")
                }
            } catch (e: Exception) {
                logger("Error in getTendersPage function", e.stackTrace, e)
            }

            return p
        }
    }
}