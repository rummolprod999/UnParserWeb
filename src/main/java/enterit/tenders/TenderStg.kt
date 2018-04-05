package enterit.tenders

import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.gargoylesoftware.htmlunit.html.HtmlSpan
import enterit.*
import enterit.parsers.ParserStg
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.sql.Timestamp
import java.util.*

class TenderStg(val status: String, val purNum: String, var urlT: String, val purObj: String, val orgFullName: String, var placingWayName: String, val regionName: String) : TenderAbstract(), ITender {
    companion object TypeFz {
        val typeFz = 27
        const val timeoutB = 5000L
    }

    init {
        etpName = "АО \"СТНГ\""
        etpUrl = "https://tender.stg.ru"
    }

    override fun parsing() {
        val page: HtmlPage = ParserStg.webClient.getPage(urlT)
        page.webClient.waitForBackgroundJavaScript(timeoutB)
        var pubDateT = page.getFirstByXPath<HtmlSpan>("//td[contains(preceding-sibling::td, 'Дата публикации')]/span")?.textContent?.trim { it <= ' ' }
                ?: ""
        pubDateT = pubDateT.regExpTest("""^(\d{2}\.\d{2}\.\d{4} \d{2}:\d{2})""")
        val pubDate = pubDateT.getDateFromString(formatterGpn)
        if (pubDate == Date(0L)) {
            logger("can not find pubDate on page", urlT)
            return
        }
        var endDateT = page.getFirstByXPath<HtmlSpan>("//td[contains(preceding-sibling::td, 'Дата окончания подачи заявок')]/span")?.textContent?.trim { it <= ' ' }
                ?: ""
        endDateT = endDateT.regExpTest("""^(\d{2}\.\d{2}\.\d{4} \d{2}:\d{2})""")
        val endDate = endDateT.getDateFromString(formatterGpn)
        if (endDate == Date(0L)) {
            logger("can not find endDate on page", urlT)
            return
        }
        val dateVer = Date()
        DriverManager.getConnection(UrlConnect, UserDb, PassDb).use(fun(con: Connection) {
            val stmt0 = con.prepareStatement("SELECT id_tender FROM ${Prefix}tender WHERE purchase_number = ? AND doc_publish_date = ? AND type_fz = ? AND notice_version = ?")
            stmt0.setString(1, purNum)
            stmt0.setTimestamp(2, Timestamp(pubDate.time))
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
            var cancelstatus = 0
            val stmt = con.prepareStatement("SELECT id_tender, date_version FROM ${Prefix}tender WHERE purchase_number = ? AND cancel=0 AND type_fz = ?")
            stmt.setString(1, purNum)
            stmt.setInt(2, typeFz)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                val idT = rs.getInt(1)
                val dateB: Timestamp = rs.getTimestamp(2)
                if (dateVer.after(dateB) || dateB == Timestamp(dateVer.time)) {
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
            if (orgFullName != "") {
                val stmto = con.prepareStatement("SELECT id_organizer FROM ${Prefix}organizer WHERE full_name = ?")
                stmto.setString(1, orgFullName)
                val rso = stmto.executeQuery()
                if (rso.next()) {
                    IdOrganizer = rso.getInt(1)
                    rso.close()
                    stmto.close()
                } else {
                    rso.close()
                    stmto.close()
                    val email = page.getFirstByXPath<HtmlSpan>("//div[contains(preceding-sibling::div, 'Ответственный за процедуру')]//td[contains(preceding-sibling::td, 'Email')]/span")?.textContent?.trim { it <= ' ' }
                            ?: ""
                    val phone = page.getFirstByXPath<HtmlSpan>("//div[contains(preceding-sibling::div, 'Ответственный за процедуру')]//td[contains(preceding-sibling::td, 'Телефон')]/span")?.textContent?.trim { it <= ' ' }
                            ?: ""
                    val contactPerson = page.getFirstByXPath<HtmlSpan>("//div[contains(preceding-sibling::div, 'Ответственный за процедуру')]//td[contains(preceding-sibling::td, 'ФИО')]/span")?.textContent?.trim { it <= ' ' }
                            ?: ""
                    val stmtins = con.prepareStatement("INSERT INTO ${Prefix}organizer SET full_name = ?, contact_email = ?, contact_phone = ?, contact_person = ?", Statement.RETURN_GENERATED_KEYS)
                    stmtins.setString(1, orgFullName)
                    stmtins.setString(2, email)
                    stmtins.setString(3, phone)
                    stmtins.setString(4, contactPerson)
                    stmtins.executeUpdate()
                    val rsoi = stmtins.generatedKeys
                    if (rsoi.next()) {
                        IdOrganizer = rsoi.getInt(1)
                    }
                    rsoi.close()
                    stmtins.close()
                }
            }
            val idEtp = getEtp(con)
            var idPlacingWay = 0
            if (placingWayName != "") {
                idPlacingWay = getPlacingWay(con, placingWayName)
            }
        })

    }
}