package enterit.tenders

import enterit.downloadFromUrl
import enterit.formatterGpn
import enterit.getDateFromFormat
import enterit.logger
import org.jsoup.Jsoup

class TenderGpn(val status: String, val url: String) {
    companion object TypeFz {
        val typeFz = 12
    }

    fun parsing(){
        val stPage = downloadFromUrl(url)
        if (stPage == "") {
            logger("Gets empty string TenderGpn", url)
            return
        }
        val html = Jsoup.parse(stPage)
        val startDateT = html.selectFirst("div:contains(Дата и время начала приёма предложений:) + div")?.ownText()?.trim { it <= ' ' } ?: ""
        val endDateT = html.selectFirst("div:contains(Дата и время окончания приёма предложений:) + div")?.ownText()?.trim { it <= ' ' } ?: ""
        val startDate = getDateFromFormat(startDateT, formatterGpn)
        val endDate = getDateFromFormat(endDateT, formatterGpn)
        println(startDate)
    }
}