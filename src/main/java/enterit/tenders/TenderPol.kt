package enterit.tenders

import enterit.*
import org.jsoup.Jsoup
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Timestamp
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
        if (startDate == Date(0L)) {
            logger("Gets empty startDate TenderPol", url)
            return
        }
        val purNum = (html.selectFirst("div.purchase-single-head > h2 > span")?.ownText()?.trim { it <= ' ' }
                ?: "").removeSurrounding("[", "]")
        if (purNum == "") {
            logger("Empty purchase number in $url")
            return
        }
        DriverManager.getConnection(UrlConnect, UserDb, PassDb).use(fun(con: Connection) {
            val stmt0 = con.prepareStatement("SELECT id_tender FROM ${Prefix}tender WHERE purchase_number = ? AND date_version = ? AND type_fz = ? AND notice_version = ? AND end_date = ?")
            stmt0.setString(1, purNum)
            stmt0.setTimestamp(2, Timestamp(startDate.time))
            stmt0.setInt(3, TenderGpn.typeFz)
            stmt0.setString(4, status)
            stmt0.setTimestamp(5, Timestamp(endDate.time))
            val r = stmt0.executeQuery()
        })
    }
}