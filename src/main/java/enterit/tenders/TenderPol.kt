package enterit.tenders

import enterit.*
import org.jsoup.Jsoup
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
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
            stmt0.setInt(3, typeFz)
            stmt0.setString(4, status)
            stmt0.setTimestamp(5, Timestamp(endDate.time))
            val r = stmt0.executeQuery()
            if (r.next()) {
                r.close()
                stmt0.close()
                return
            }
            r.close()
            stmt0.close()
            var cancelstatus = 0
            val stmt = con.prepareStatement("SELECT id_tender, date_version FROM ${Prefix}tender WHERE purchase_number = ? AND cancel=0 AND type_fz = ?")
            stmt.setString(1, purNum)
            stmt.setInt(2, typeFz)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                val idT = rs.getInt(1)
                val dateB: Timestamp = rs.getTimestamp(2)
                if (startDate.after(dateB) || dateB == Timestamp(startDate.time)) {
                    val preparedStatement = con.prepareStatement("UPDATE ${Prefix}tender SET cancel=1 WHERE id_tender = ?")
                    preparedStatement.setInt(1, idT)
                    preparedStatement.execute()
                    preparedStatement.close()
                } else {
                    cancelstatus = 1
                }
            }
            rs.close()
            stmt.close()
            var IdOrganizer = 0
            val fullnameOrg = html.selectFirst("div.purchase-single-filters > a")?.ownText()?.trim { it <= ' ' } ?: ""
            if (fullnameOrg != "") {
                val stmto = con.prepareStatement("SELECT id_organizer FROM ${Prefix}organizer WHERE full_name = ?")
                stmto.setString(1, fullnameOrg)
                val rso = stmto.executeQuery()
                if (rso.next()) {
                    IdOrganizer = rso.getInt(1)
                    rso.close()
                    stmto.close()
                } else {
                    rso.close()
                    stmto.close()
                    val phone = html.selectFirst("div:contains(Контактный телефон:) + div")?.text()?.trim { it <= ' ' }
                            ?: ""
                    val email = html.selectFirst("div:contains(Контактный e-mail:) + div")?.text()?.trim { it <= ' ' }
                            ?: ""
                    val stmtins = con.prepareStatement("INSERT INTO ${Prefix}organizer SET full_name = ?, contact_email = ?, contact_phone = ?", Statement.RETURN_GENERATED_KEYS)
                    stmtins.setString(1, fullnameOrg)
                    stmtins.setString(2, email)
                    stmtins.setString(3, phone)
                    stmtins.executeUpdate()
                    val rsoi = stmtins.generatedKeys
                    if (rsoi.next()) {
                        IdOrganizer = rsoi.getInt(1)
                    }
                    rsoi.close()
                    stmtins.close()
                }
            }
        })
    }
}