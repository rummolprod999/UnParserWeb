package enterit.tenders

import enterit.*
import org.jsoup.Jsoup
import java.sql.*
import java.util.Date

class TenderEtpRf(val status: String, val entNum: String, var purNum: String, val purObj: String, val nmck: String, val placingWay: String, val datePub: Date, val dateEnd: Date, val url: String) {
    companion object TypeFz {
        val typeFz = 12
    }

    fun parsing() {
        if (purNum == "") {
            purNum = entNum
        }
        DriverManager.getConnection(UrlConnect, UserDb, PassDb).use(fun(con: Connection) {
            val stmt0 = con.prepareStatement("SELECT id_tender FROM ${Prefix}tender WHERE purchase_number = ? AND date_version = ? AND type_fz = ? AND notice_version = ?")
            stmt0.setString(1, purNum)
            stmt0.setTimestamp(2, Timestamp(datePub.time))
            stmt0.setInt(3, typeFz)
            stmt0.setString(4, status)
            val r = stmt0.executeQuery()
            if (r.next()) {
                r.close()
                stmt0.close()
                return
            }
            r.close()
            stmt0.close()
            val stPage = downloadFromUrl(url)
            if (stPage == "") {
                logger("Gets empty string TenderEtpRf", url)
                return
            }
            val html = Jsoup.parse(stPage)
            var cancelstatus = 0
            val stmt = con.prepareStatement("SELECT id_tender, date_version FROM ${Prefix}tender WHERE purchase_number = ? AND cancel=0 AND type_fz = ?")
            stmt.setString(1, purNum)
            stmt.setInt(2, typeFz)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                val idT = rs.getInt(1)
                val dateB: Timestamp = rs.getTimestamp(2)
                if (datePub.after(dateB) || dateB == Timestamp(datePub.time)) {
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
        })
    }
}