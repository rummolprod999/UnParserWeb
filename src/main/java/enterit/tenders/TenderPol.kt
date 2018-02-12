package enterit.tenders

import enterit.GetDates
import enterit.downloadFromUrl
import enterit.logger
import org.jsoup.Jsoup
import java.util.*

data class TenderInfoPol(val startDate: Date, val endDate: Date, val status: String)
class TenderPol(val url: String) {
    companion object TypeFz {
        val typeFz = 14
    }

    fun parsing() {
        val stPage = downloadFromUrl(url)
        if (stPage == "") {
            logger("Gets empty string TenderPol", url)
            return
        }
        val html = Jsoup.parse(stPage)
        val detail = html.selectFirst("div.purchase-single-info")?.ownText()?.trim { it <= ' ' } ?: ""
        val (startDate, endDate, status) = this.GetDates(detail)
        println(status)
    }
}