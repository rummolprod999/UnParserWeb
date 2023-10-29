package enterit.tenders

import enterit.*
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.sql.Timestamp
import java.util.*

private const val url = "https://www.sibur.ru/procurement/buy"

class TenderSiburNew(
    val purNum: String,
    val purName: String,
    val purObj: String,
    val placingWay: String,
    val startDate: Date,
    val endDate: Date,
    val orgName: String,
    val status: String,
    val contactPerson: String,
    val phone: String,
    val email: String
) {
    companion object O {
        const val typeFz = 18
    }

    fun parsing() {
        if (purNum == "") {
            logger("Empty purchase number")
            return
        }
        DriverManager.getConnection(UrlConnect, UserDb, PassDb).use(fun(con: Connection) {
            val stmt0 =
                con.prepareStatement("SELECT id_tender FROM ${Prefix}tender WHERE purchase_number = ? AND date_version = ? AND type_fz = ? AND notice_version = ? AND end_date = ?")
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
            var update = false
            val stmt =
                con.prepareStatement("SELECT id_tender, date_version FROM ${Prefix}tender WHERE purchase_number = ? AND cancel=0 AND type_fz = ?")
            stmt.setString(1, purNum)
            stmt.setInt(2, TenderGpn.typeFz)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                update = true
                val idT = rs.getInt(1)
                val dateB: Timestamp = rs.getTimestamp(2)
                if (startDate.after(dateB) || dateB == Timestamp(startDate.time)) {
                    val preparedStatement =
                        con.prepareStatement("UPDATE ${Prefix}tender SET cancel=1 WHERE id_tender = ?")
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
            if (orgName != "") {
                val stmto = con.prepareStatement("SELECT id_organizer FROM ${Prefix}organizer WHERE full_name = ?")
                stmto.setString(1, orgName)
                val rso = stmto.executeQuery()
                if (rso.next()) {
                    IdOrganizer = rso.getInt(1)
                    rso.close()
                    stmto.close()
                } else {
                    rso.close()
                    stmto.close()
                    val stmtins = con.prepareStatement(
                        "INSERT INTO ${Prefix}organizer SET full_name = ?, inn = ?, kpp = ?, post_address = ?, contact_person = ?, contact_email = ?, contact_phone = ?, contact_fax = ?",
                        Statement.RETURN_GENERATED_KEYS
                    )
                    stmtins.setString(1, orgName)
                    stmtins.setString(2, "")
                    stmtins.setString(3, "")
                    stmtins.setString(4, "")
                    stmtins.setString(5, contactPerson)
                    stmtins.setString(6, email)
                    stmtins.setString(7, phone)
                    stmtins.setString(8, "")
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
            val etpName = "ПАО «СИБУР Холдинг»"
            val etpUrl = "http://b2b.sibur.ru"
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
                    val stmtins = con.prepareStatement(
                        "INSERT INTO ${Prefix}etp SET name = ?, url = ?, conf=0",
                        Statement.RETURN_GENERATED_KEYS
                    )
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
            if (placingWay != "") {
                val stmto =
                    con.prepareStatement("SELECT id_placing_way FROM ${Prefix}placing_way WHERE name = ? LIMIT 1")
                stmto.setString(1, placingWay)
                val rso = stmto.executeQuery()
                if (rso.next()) {
                    IdPlacingWay = rso.getInt(1)
                    rso.close()
                    stmto.close()
                } else {
                    rso.close()
                    stmto.close()
                    val conf = getConformity(placingWay)
                    val stmtins = con.prepareStatement(
                        "INSERT INTO ${Prefix}placing_way SET name = ?, conformity = ?",
                        Statement.RETURN_GENERATED_KEYS
                    )
                    stmtins.setString(1, placingWay)
                    stmtins.setInt(2, conf)
                    stmtins.executeUpdate()
                    val rsoi = stmtins.generatedKeys
                    if (rsoi.next()) {
                        IdPlacingWay = rsoi.getInt(1)
                    }
                    rsoi.close()
                    stmtins.close()

                }
            }
            var idTender = 0
            val insertTender = con.prepareStatement(
                "INSERT INTO ${Prefix}tender SET id_xml = ?, purchase_number = ?, doc_publish_date = ?, href = ?, purchase_object_info = ?, type_fz = ?, id_organizer = ?, id_placing_way = ?, id_etp = ?, end_date = ?, cancel = ?, date_version = ?, num_version = ?, notice_version = ?, xml = ?, print_form = ?, id_region = 0, scoring_date = ?",
                Statement.RETURN_GENERATED_KEYS
            )
            insertTender.setString(1, purNum)
            insertTender.setString(2, purNum)
            insertTender.setTimestamp(3, Timestamp(startDate.time))
            insertTender.setString(4, url)
            insertTender.setString(5, purName)
            insertTender.setInt(6, typeFz)
            insertTender.setInt(7, IdOrganizer)
            insertTender.setInt(8, IdPlacingWay)
            insertTender.setInt(9, IdEtp)
            insertTender.setTimestamp(10, Timestamp(endDate.time))
            insertTender.setInt(11, cancelstatus)
            insertTender.setTimestamp(12, Timestamp(startDate.time))
            insertTender.setInt(13, 1)
            insertTender.setString(14, status)
            insertTender.setString(15, url)
            insertTender.setString(16, url)
            insertTender.setTimestamp(17, Timestamp(endDate.time))
            insertTender.executeUpdate()
            val rt = insertTender.generatedKeys
            if (rt.next()) {
                idTender = rt.getInt(1)
            }
            rt.close()
            insertTender.close()
            if (update) {
                UpTenderSibur++
            } else {
                AddTenderSibur++
            }
            var idLot = 0
            val LotNumber = 1
            val insertLot = con.prepareStatement(
                "INSERT INTO ${Prefix}lot SET id_tender = ?, lot_number = ?, currency = ?",
                Statement.RETURN_GENERATED_KEYS
            )
            insertLot.setInt(1, idTender)
            insertLot.setInt(2, LotNumber)
            insertLot.setString(3, "")
            insertLot.executeUpdate()
            val rl = insertLot.generatedKeys
            if (rl.next()) {
                idLot = rl.getInt(1)
            }
            rl.close()
            insertLot.close()
            var idCustomer = 0
            val fullnameCus = orgName
            if (fullnameCus != "") {
                val stmto =
                    con.prepareStatement("SELECT id_customer FROM ${Prefix}customer WHERE full_name = ? LIMIT 1")
                stmto.setString(1, fullnameCus)
                val rso = stmto.executeQuery()
                if (rso.next()) {
                    idCustomer = rso.getInt(1)
                    rso.close()
                    stmto.close()
                } else {
                    rso.close()
                    stmto.close()
                    val stmtins = con.prepareStatement(
                        "INSERT INTO ${Prefix}customer SET full_name = ?, is223=1, reg_num = ?",
                        Statement.RETURN_GENERATED_KEYS
                    )
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
            val insertPurObj =
                con.prepareStatement("INSERT INTO ${Prefix}purchase_object SET id_lot = ?, id_customer = ?, name = ?")
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