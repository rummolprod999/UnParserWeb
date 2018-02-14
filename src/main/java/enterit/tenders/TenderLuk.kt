package enterit.tenders

import com.gargoylesoftware.htmlunit.html.HtmlAnchor
import com.gargoylesoftware.htmlunit.html.HtmlDivision
import com.gargoylesoftware.htmlunit.html.HtmlSpan
import enterit.*
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.sql.Timestamp
import java.util.*

class TenderLuk(private val p: HtmlDivision) {
    companion object O {
        const val typeFz = 15
        const val url = "http://www.lukoil.ru/Company/Tendersandauctions/Tenders"
    }

    fun parser() {
        var purObj = ""
        val purObjA = p.getElementsByTagName("h2")
        if (!purObjA.isEmpty()) {
            purObj = purObjA[0].textContent.trim { it <= ' ' }
        }
        var purNum = ""
        val purNumT = p.getByXPath<HtmlSpan>(".//div[@class = 'item']//span")
        if (!purNumT.isEmpty()) {
            purNum = purNumT[0].textContent.trim { it <= ' ' }
        }
        if (purNum == "") {
            logger("Empty purchase number in $url")
            return
        }
        DriverManager.getConnection(UrlConnect, UserDb, PassDb).use(fun(con: Connection) {
            val stmt0 = con.prepareStatement("SELECT id_tender FROM ${Prefix}tender WHERE purchase_number = ? AND type_fz = ?")
            stmt0.setString(1, purNum)
            stmt0.setInt(2, typeFz)
            val r = stmt0.executeQuery()
            if (r.next()) {
                r.close()
                stmt0.close()
                return
            }
            r.close()
            stmt0.close()
            var cancelstatus = 0
            val startDate = Date()
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
            var fullnameOrg = ""
            val fullnameOrgT = p.getByXPath<HtmlSpan>(".//span[@data-bind = 'text: OrganizerName']")
            if (!fullnameOrgT.isEmpty()) {
                fullnameOrg = fullnameOrgT[0].textContent.trim { it <= ' ' }
            }
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

                    val stmtins = con.prepareStatement("INSERT INTO ${Prefix}organizer SET full_name = ?", Statement.RETURN_GENERATED_KEYS)
                    stmtins.setString(1, fullnameOrg)
                    stmtins.executeUpdate()
                    val rsoi = stmtins.generatedKeys
                    if (rsoi.next()) {
                        IdOrganizer = rsoi.getInt(1)
                    }
                    rsoi.close()
                    stmtins.close()
                }
            }
            var IdEtp = 0
            val etpName = "ПАО «ЛУКОЙЛ»"
            val etpUrl = "http://www.lukoil.ru/"
            try {
                val stmto = con.prepareStatement("SELECT id_etp FROM ${Prefix}etp WHERE name = ? AND url = ? LIMIT 1")
                stmto.setString(1, etpName)
                stmto.setString(2, etpUrl)
                val rso = stmto.executeQuery()
                if (rso.next()) {
                    IdEtp = rso.getInt(1)
                    rso.close()
                    stmto.close()
                } else {
                    rso.close()
                    stmto.close()
                    val stmtins = con.prepareStatement("INSERT INTO ${Prefix}etp SET name = ?, url = ?, conf=0", Statement.RETURN_GENERATED_KEYS)
                    stmtins.setString(1, etpName)
                    stmtins.setString(2, etpUrl)
                    stmtins.executeUpdate()
                    val rsoi = stmtins.generatedKeys
                    if (rsoi.next()) {
                        IdEtp = rsoi.getInt(1)
                    }
                    rsoi.close()
                    stmtins.close()
                }
            } catch (ignored: Exception) {

            }
            var IdPlacingWay = 0
            var idTender = 0
            var endDateTemp = ""
            val endDateT = p.getByXPath<HtmlSpan>(".//span[@data-bind = \"text: moment(DateFinish).format('L')\"]")
            if (!endDateT.isEmpty()) {
                endDateTemp = endDateT[0].textContent.trim { it <= ' ' }
            }
            val endDate = getDateFromFormat(endDateTemp, formatterOnlyDate)
            val insertTender = con.prepareStatement("INSERT INTO ${Prefix}tender SET id_region = 0, id_xml = ?, purchase_number = ?, doc_publish_date = ?, href = ?, purchase_object_info = ?, type_fz = ?, id_organizer = ?, id_placing_way = ?, id_etp = ?, end_date = ?, cancel = ?, date_version = ?, num_version = ?, xml = ?, print_form = ?", Statement.RETURN_GENERATED_KEYS)
            insertTender.setString(1, purNum)
            insertTender.setString(2, purNum)
            insertTender.setTimestamp(3, Timestamp(startDate.time))
            insertTender.setString(4, url)
            insertTender.setString(5, purObj)
            insertTender.setInt(6, typeFz)
            insertTender.setInt(7, IdOrganizer)
            insertTender.setInt(8, IdPlacingWay)
            insertTender.setInt(9, IdEtp)
            insertTender.setTimestamp(10, Timestamp(endDate.time))
            insertTender.setInt(11, cancelstatus)
            insertTender.setTimestamp(12, Timestamp(startDate.time))
            insertTender.setInt(13, 1)
            insertTender.setString(14, url)
            insertTender.setString(15, url)
            insertTender.executeUpdate()
            val rt = insertTender.generatedKeys
            if (rt.next()) {
                idTender = rt.getInt(1)
            }
            rt.close()
            insertTender.close()
            AddTenderLuk++
            val documents = p.getByXPath<HtmlAnchor>(".//a[@data-bind = \"text: Title, attr: { href: FileDownloadUrl }\"]")
            for (d in documents) {
                val nameDoc = d?.textContent?.trim { it <= ' ' } ?: ""
                val href = d?.getAttribute("href")?.trim { it <= ' ' } ?: ""
                if (href != "") {
                    val insertDoc = con.prepareStatement("INSERT INTO ${Prefix}attachment SET id_tender = ?, file_name = ?, url = ?")
                    insertDoc.setInt(1, idTender)
                    insertDoc.setString(2, nameDoc)
                    insertDoc.setString(3, href)
                    insertDoc.executeUpdate()
                    insertDoc.close()
                }
            }
            var idLot = 0
            val lotNum = 1
            val insertLot = con.prepareStatement("INSERT INTO ${Prefix}lot SET id_tender = ?, lot_number = ?", Statement.RETURN_GENERATED_KEYS)
            insertLot.setInt(1, idTender)
            insertLot.setInt(2, lotNum)
            insertLot.executeUpdate()
            val rl = insertLot.generatedKeys
            if (rl.next()) {
                idLot = rl.getInt(1)
            }
            rl.close()
            insertLot.close()
            var idCustomer = 0
            var fullnameCus = ""
            val fullnameCusT = p.getByXPath<HtmlSpan>(".//span[@data-bind = 'text: Organization.Name']")
            if (!fullnameCusT.isEmpty()) {
                fullnameCus = fullnameCusT[0].textContent.trim { it <= ' ' }
            }
            if (fullnameCus != "") {
                val stmto = con.prepareStatement("SELECT id_customer FROM ${Prefix}customer WHERE full_name = ? LIMIT 1")
                stmto.setString(1, fullnameCus)
                val rso = stmto.executeQuery()
                if (rso.next()) {
                    idCustomer = rso.getInt(1)
                    rso.close()
                    stmto.close()
                } else {
                    rso.close()
                    stmto.close()
                    val stmtins = con.prepareStatement("INSERT INTO ${Prefix}customer SET full_name = ?, is223=1, reg_num = ?", Statement.RETURN_GENERATED_KEYS)
                    stmtins.setString(1, fullnameCus)
                    stmtins.setString(2, java.util.UUID.randomUUID().toString())
                    stmtins.executeUpdate()
                    val rsoi = stmtins.generatedKeys
                    if (rsoi.next()) {
                        idCustomer = rsoi.getInt(1)
                    }
                    rsoi.close()
                    stmtins.close()
                }
            }
            val insertPurObj = con.prepareStatement("INSERT INTO ${Prefix}purchase_object SET id_lot = ?, id_customer = ?, name = ?")
            insertPurObj.setInt(1, idLot)
            insertPurObj.setInt(2, idCustomer)
            insertPurObj.setString(3, purObj)
            insertPurObj.executeUpdate()
            insertPurObj.close()
            try {
                tenderKwords(idTender, con)
            } catch (e: Exception) {
                logger("Ошибка добавления ключевых слов", e.stackTrace, e)
            }
            try {
                addVNum(con, purNum, typeFz)
            } catch (e: Exception) {
                logger("Ошибка добавления версий", e.stackTrace, e)
            }
        })
    }
}