package enterit.parsers

import enterit.downloadFromUrl
import enterit.logger
import enterit.regExpTest
import enterit.tenders.TenderPolNew
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class ParserPolNew : Iparser {
    companion object BaseTen {
        const val BaseT = "http://tenders.polyusgold.com/"
    }

    override fun parser() {
        val stPage = downloadFromUrl(BaseT)
        if (stPage == "") {
            logger("Gets empty string ParserPol", BaseT)
            return
        }
        val html = Jsoup.parse(stPage)
        val tenders = html.select("table.my_table > tbody > tr")
        tenders.forEach(::parsingTender)

    }

    private fun parsingTender(e: Element) {
        val purName = e.selectFirst("td:eq(1)  p > a")?.text()?.trim { it <= ' ' }
                ?: run { logger("purName not found"); return }
        val urlT = e.selectFirst("td:eq(1)  p > a")?.attr("href")?.trim { it <= ' ' }
                ?: run { logger("urlT not found on $purName"); return }
        val urlTend = "http://tenders.polyusgold.com$urlT"
        val purNum = purName.regExpTest("""^(\[.+\])\s""")
        if (purNum == "") {
            logger("purNum not found in $purName")
        }
        val status = e.selectFirst("td:eq(2) > a")?.text()?.trim { it <= ' ' }
                ?: run { logger("status not found in $purNum"); return }
        val orgName = e.selectFirst("td:eq(1)  div > a")?.text()?.trim { it <= ' ' }
                ?: run { logger("purName not found"); "" }
        val t = TenderPolNew(urlTend, purName, purNum, status, orgName)
        t.parsing()
    }
}