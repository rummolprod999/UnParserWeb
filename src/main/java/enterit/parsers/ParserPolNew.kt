package enterit.parsers

import enterit.downloadFromUrl
import enterit.logger
import enterit.tenders.TenderPolNew
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class ParserPolNew : Iparser {

    private val maxPage = 4
    private val baseUrl = "https://tenders.polyus.com/purchases/?&PAGEN_1="
    override fun parser() = (maxPage downTo 1)
        .map { "$baseUrl$it" }
        .forEach {
            try {
                parserPage(it)
            } catch (e: Exception) {
                logger("Error in ParserPol.parser function", e.stackTrace, e)
            }
        }

    fun parserPage(url: String) {
        val stPage = downloadFromUrl(url)
        if (stPage == "") {
            logger("Gets empty string ParserPol", url)
            return
        }
        val html = Jsoup.parse(stPage)
        val tenders = html.select("table.my_table > tbody > tr")
        tenders.forEach(::parsingTender)

    }

    private fun parsingTender(e: Element) {
        val purName = e.selectFirst("td:eq(2) p > a")?.text()?.trim { it <= ' ' }
            ?: run { logger("purName not found"); return }
        val urlT = e.selectFirst("tr a[href^='detail.php']")?.attr("href")?.trim { it <= ' ' }
            ?: run { logger("urlT not found on $purName"); return }
        val urlTend = "https://tenders.polyus.com/purchases/$urlT"
        val purNum = e.selectFirst("tr a[href^='detail.php']")?.attr("title")?.trim { it <= ' ' } ?: ""
        if (purNum == "") {
            logger("purNum not found in $purName")
        }
        val status = e.selectFirst("td:eq(3) > a")?.text()?.trim { it <= ' ' }
            ?: run { logger("status not found in $purNum"); return }
        val orgName = e.selectFirst("td:eq(2) div > a")?.text()?.trim { it <= ' ' }
            ?: run { logger("orgName not found"); "" }
        val t = TenderPolNew(urlTend, purName, purNum, status, orgName)
        t.parsing()
    }
}