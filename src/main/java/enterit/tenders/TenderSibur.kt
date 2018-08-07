package enterit.tenders

import enterit.*
import org.jsoup.Jsoup
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.sql.Timestamp
import java.util.*

class TenderSibur(val urlTend: String, val purNum: String, val currency: String) {
    companion object O {
        const val typeFz = 18
    }

    fun parsing() {
        if (purNum == "") {
            logger("Empty purchase number in $urlTend")
            return
        }
        val stPage = downloadFromUrl(urlTend, wt = 10000)
        if (stPage == "") {
            logger("Gets empty string TenderEtpRf", urlTend)
            return
        }
        val html = Jsoup.parse(stPage)
        val startDateT = html.selectFirst("div:containsOwn(Конкурентная процедура объявлена:) + div")?.ownText()?.trim { it <= ' ' }
                ?: ""
        val datePub = getDateFromFormat(startDateT, formatterGpn)
        val endDateT = html.selectFirst("div:containsOwn(Дата вскрытия конвертов:) + div")?.ownText()?.trim { it <= ' ' }
                ?: ""
        val dateEnd = getDateFromFormat(endDateT, formatterGpn)
        val VerDateT = html.selectFirst("div:containsOwn(Дата последнего редактирования:) + div")?.ownText()?.trim { it <= ' ' }
                ?: ""
        var dateVer = getDateFromFormat(VerDateT, formatterGpn)
        if (dateVer == Date(0L)) {
            dateVer = datePub
        }
        DriverManager.getConnection(UrlConnect, UserDb, PassDb).use(fun(con: Connection) {
            val stmt0 = con.prepareStatement("SELECT id_tender FROM ${Prefix}tender WHERE purchase_number = ? AND date_version = ? AND type_fz = ?")
            stmt0.setString(1, purNum)
            stmt0.setTimestamp(2, Timestamp(dateVer.time))
            stmt0.setInt(3, typeFz)
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
            val stmt = con.prepareStatement("SELECT id_tender, date_version FROM ${Prefix}tender WHERE purchase_number = ? AND cancel=0 AND type_fz = ?")
            stmt.setString(1, purNum)
            stmt.setInt(2, typeFz)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                update = true
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
            val fullnameOrg = html.selectFirst("div:containsOwn(Организатор конкурентной процедуры:) + div")?.ownText()?.trim { it <= ' ' }
                    ?: ""
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
            val PlacingWayT = html.selectFirst("div:containsOwn(Вид процедуры:) + div")?.ownText()?.trim { it <= ' ' }
                    ?: ""
            var placingWay = ""
            when {
                PlacingWayT.contains("Запрос") -> {
                    placingWay = "Запрос предложений"
                }
                PlacingWayT.contains("Тендер") -> {
                    placingWay = "Тендер"
                }
                else -> {
                    logger("placing way not known", PlacingWayT)
                }
            }
            if (placingWay != "") {
                val stmto = con.prepareStatement("SELECT id_placing_way FROM ${Prefix}placing_way WHERE name = ? LIMIT 1")
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
                    val stmtins = con.prepareStatement("INSERT INTO ${Prefix}placing_way SET name = ?, conformity = ?", Statement.RETURN_GENERATED_KEYS)
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
            val purObj = html.selectFirst("div:containsOwn(Предмет конкурентной процедуры:) + div")?.ownText()?.trim { it <= ' ' }
                    ?: ""
            val noticeVer = html.selectFirst("div:containsOwn(Дополнительная информация:) + div")?.ownText()?.trim { it <= ' ' }
                    ?: ""
            var idTender = 0
            val insertTender = con.prepareStatement("INSERT INTO ${Prefix}tender SET id_xml = ?, purchase_number = ?, doc_publish_date = ?, href = ?, purchase_object_info = ?, type_fz = ?, id_organizer = ?, id_placing_way = ?, id_etp = ?, end_date = ?, cancel = ?, date_version = ?, num_version = ?, notice_version = ?, xml = ?, print_form = ?, id_region = 0, scoring_date = ?", Statement.RETURN_GENERATED_KEYS)
            insertTender.setString(1, purNum)
            insertTender.setString(2, purNum)
            insertTender.setTimestamp(3, Timestamp(datePub.time))
            insertTender.setString(4, urlTend)
            insertTender.setString(5, purObj)
            insertTender.setInt(6, typeFz)
            insertTender.setInt(7, IdOrganizer)
            insertTender.setInt(8, IdPlacingWay)
            insertTender.setInt(9, IdEtp)
            insertTender.setTimestamp(10, Timestamp(dateEnd.time))
            insertTender.setInt(11, cancelstatus)
            insertTender.setTimestamp(12, Timestamp(datePub.time))
            insertTender.setInt(13, 1)
            insertTender.setString(14, noticeVer)
            insertTender.setString(15, urlTend)
            insertTender.setString(16, urlTend)
            insertTender.setTimestamp(17, Timestamp(dateEnd.time))
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
            val insertLot = con.prepareStatement("INSERT INTO ${Prefix}lot SET id_tender = ?, lot_number = ?, currency = ?", Statement.RETURN_GENERATED_KEYS)
            insertLot.setInt(1, idTender)
            insertLot.setInt(2, LotNumber)
            insertLot.setString(3, currency)
            insertLot.executeUpdate()
            val rl = insertLot.generatedKeys
            if (rl.next()) {
                idLot = rl.getInt(1)
            }
            rl.close()
            insertLot.close()
            var idCustomer = 0
            val fullnameCus = fullnameOrg
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
            val delivPlace = html.selectFirst("div:containsOwn(Адрес места поставки товара, проведения работ или оказания услуг:) + div")?.text()?.trim { it <= ' ' }
                    ?: ""
            if (delivPlace != "") {
                val delivTermT = html.selectFirst("div:containsOwn(Сроки поставки) + div")?.text()?.trim { it <= ' ' }
                        ?: ""
                val delivTerm = "Сроки поставки (выполнения работ): $delivTermT"
                val insertCusRec = con.prepareStatement("INSERT INTO ${Prefix}customer_requirement SET id_lot = ?, id_customer = ?, delivery_place = ?, delivery_term = ?")
                insertCusRec.setInt(1, idLot)
                insertCusRec.setInt(2, idCustomer)
                insertCusRec.setString(3, delivPlace)
                insertCusRec.setString(4, delivTerm)
                insertCusRec.executeUpdate()
                insertCusRec.close()
            }
            val restr = html.selectFirst("div:containsOwn(Требования к участникам:) + div")?.text()?.trim { it <= ' ' }
                    ?: ""
            if (restr != "") {
                val insertRestr = con.prepareStatement("INSERT INTO ${Prefix}restricts SET id_lot = ?, foreign_info = ?")
                insertRestr.setInt(1, idLot)
                insertRestr.setString(2, restr)
                insertRestr.executeUpdate()
                insertRestr.close()
            }

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