package enterit.tenders

import enterit.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.sql.Timestamp
import java.util.*

data class TenderZakupki(val purNum: String, var urlT: String, val purObj: String, val region: String, val datePub: Date, val dateEnd: Date) : TenderAbstract(), ITender {
    companion object TypeFz {
        val typeFz = 63
    }

    init {
        etpName = "Торгово-закупочная система Zakupki.ru"
        etpUrl = "https://www.zakupki.ru"
    }

    override fun parsing() {
        val dateVer = Date()
        DriverManager.getConnection(UrlConnect, UserDb, PassDb).use(fun(con: Connection) {
            val stmt0 = con.prepareStatement("SELECT id_tender FROM ${Prefix}tender WHERE purchase_number = ? AND doc_publish_date = ? AND type_fz = ? AND end_date = ?")
            stmt0.setString(1, purNum)
            stmt0.setTimestamp(2, Timestamp(datePub.time))
            stmt0.setInt(3, typeFz)
            stmt0.setTimestamp(4, Timestamp(dateEnd.time))
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
            val stPage = downloadFromUrl1251(urlT)
            if (stPage == "") {
                logger("Gets empty string TenderZakupki", urlT)
                return
            }
            val html: Document = Jsoup.parse(stPage)
            val orgFullName = html.selectFirst("td:containsOwn(Организатор процедуры:) + td")?.ownText()?.trim { it <= ' ' }
                    ?: ""
            if (orgFullName != "" && !orgFullName.contains("Информация скрыта")) {

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
                    val email = ""
                    val phone = ""
                    val contactPerson = ""
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
            val idPlacingWay = 0
            var idRegion = 0
            if (region != "") {
                idRegion = getIdRegion(con, region)
            }
            val notVer1 = html.selectFirst("td:containsOwn(Краткий текст:) + td")?.ownText()?.trim { it <= ' ' }
                    ?: ""
            val notVer2 = html.selectFirst("td:containsOwn(Полный текст:) + td")?.ownText()?.trim { it <= ' ' }
                    ?: ""
            var noticeVer = ""
            if (notVer1 != "" || notVer2 != "") noticeVer = "$notVer1 $notVer2".trim { it <= ' ' }
            var idTender = 0
            val insertTender = con.prepareStatement("INSERT INTO ${Prefix}tender SET id_xml = ?, purchase_number = ?, doc_publish_date = ?, href = ?, purchase_object_info = ?, type_fz = ?, id_organizer = ?, id_placing_way = ?, id_etp = ?, end_date = ?, cancel = ?, date_version = ?, num_version = ?, notice_version = ?, xml = ?, print_form = ?, id_region = ?, scoring_date = ?", Statement.RETURN_GENERATED_KEYS)
            insertTender.setString(1, purNum)
            insertTender.setString(2, purNum)
            insertTender.setTimestamp(3, Timestamp(datePub.time))
            insertTender.setString(4, urlT)
            insertTender.setString(5, purObj)
            insertTender.setInt(6, typeFz)
            insertTender.setInt(7, IdOrganizer)
            insertTender.setInt(8, idPlacingWay)
            insertTender.setInt(9, idEtp)
            insertTender.setTimestamp(10, Timestamp(dateEnd.time))
            insertTender.setInt(11, cancelstatus)
            insertTender.setTimestamp(12, Timestamp(dateVer.time))
            insertTender.setInt(13, 1)
            insertTender.setString(14, noticeVer)
            insertTender.setString(15, urlT)
            insertTender.setString(16, urlT)
            insertTender.setInt(17, idRegion)
            insertTender.setTimestamp(18, Timestamp(Date(0L).time))
            insertTender.executeUpdate()
            val rt = insertTender.generatedKeys
            if (rt.next()) {
                idTender = rt.getInt(1)
            }
            rt.close()
            insertTender.close()
            if (update) {
                UpTenderZakupki++
            } else {
                AddTenderZakupki++
            }
            var tehDoc = stPage.regExpTest("location\\.href = '(/prices/[_\\w.]+)'; ")
            if (tehDoc != "") {
                tehDoc = "https://zakupki.ru$tehDoc"
                val insertDoc = con.prepareStatement("INSERT INTO ${Prefix}attachment SET id_tender = ?, file_name = ?, url = ?")
                insertDoc.setInt(1, idTender)
                insertDoc.setString(2, "Документация")
                insertDoc.setString(3, tehDoc)
                insertDoc.executeUpdate()
                insertDoc.close()
            }
            var idCustomer = 0
            if (orgFullName != "" && !orgFullName.contains("Информация скрыта")) {
                val stmtoc = con.prepareStatement("SELECT id_customer FROM ${Prefix}customer WHERE full_name = ? LIMIT 1")
                stmtoc.setString(1, orgFullName)
                val rsoc = stmtoc.executeQuery()
                if (rsoc.next()) {
                    idCustomer = rsoc.getInt(1)
                    rsoc.close()
                    stmtoc.close()
                } else {
                    rsoc.close()
                    stmtoc.close()
                    val stmtins = con.prepareStatement("INSERT INTO ${Prefix}customer SET full_name = ?, is223=1, reg_num = ?", Statement.RETURN_GENERATED_KEYS)
                    stmtins.setString(1, orgFullName)
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
            val lots = html.select("h4:containsOwn(Лоты) + table tbody tr:gt(0)")
            if (lots.isNotEmpty()) {
                lots.forEach {
                    try {
                        val numLot = it.selectFirst("td:eq(0)")?.ownText()?.trim { it <= ' ' }
                                ?: ""
                        var nameLot = it.selectFirst("td:eq(1)")?.ownText()?.trim { it <= ' ' }
                                ?: ""
                        val okei = it.selectFirst("td:eq(2)")?.ownText()?.trim { it <= ' ' }
                                ?: ""
                        val quant = it.selectFirst("td:eq(3)")?.ownText()?.trim { it <= ' ' }
                                ?: ""
                        val desc = it.selectFirst("td:eq(4)")?.ownText()?.trim { it <= ' ' }
                                ?: ""
                        val gost = it.selectFirst("td:eq(5)")?.ownText()?.trim { it <= ' ' }
                                ?: ""
                        nameLot = "$nameLot $desc $gost".trim { it <= ' ' }
                        parsingLots(numLot = if (numLot != "") numLot else "1", nameLot = nameLot, okei = okei, quant = quant, idTender = idTender, idCustomer = idCustomer, html = html, con = con)
                    } catch (e: Exception) {
                        logger("error in ${this::class.simpleName}.parsingLots()", e.stackTrace, e, urlT)
                    }
                }
            } else {
                try {
                    parsingLots(nameLot = purObj, idTender = idTender, idCustomer = idCustomer, html = html, con = con)
                } catch (e: Exception) {
                    logger("error in ${this::class.simpleName}.parsingLots()", e.stackTrace, e, urlT)
                }
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

    private fun parsingLots(numLot: String = "1", nameLot: String, okei: String = "", quant: String = "", idTender: Int, idCustomer: Int, html: Document, con: Connection) {
        var idLot = 0
        val insertLot = con.prepareStatement("INSERT INTO ${Prefix}lot SET id_tender = ?, lot_number = ?", Statement.RETURN_GENERATED_KEYS)
        insertLot.setInt(1, idTender)
        insertLot.setString(2, numLot)
        insertLot.executeUpdate()
        val rl = insertLot.generatedKeys
        if (rl.next()) {
            idLot = rl.getInt(1)
        }
        rl.close()
        insertLot.close()
        val insertPurObj = con.prepareStatement("INSERT INTO ${Prefix}purchase_object SET id_lot = ?, id_customer = ?, name = ?, quantity_value = ?, okei = ?, customer_quantity_value = ?")
        insertPurObj.setInt(1, idLot)
        insertPurObj.setInt(2, idCustomer)
        insertPurObj.setString(3, nameLot)
        insertPurObj.setString(4, quant)
        insertPurObj.setString(5, okei)
        insertPurObj.setString(6, quant)
        insertPurObj.executeUpdate()
        insertPurObj.close()
        val delivTerm = html.selectFirst("td:containsOwn(Условия поставки:) + td")?.ownText()?.trim { it <= ' ' }
                ?: ""
        val delivPlace = html.selectFirst("td:containsOwn(Место поставки товара:) + td")?.ownText()?.trim { it <= ' ' }
                ?: ""
        if (delivTerm != "" || delivPlace != "") {
            val insertCusRec = con.prepareStatement("INSERT INTO ${Prefix}customer_requirement SET id_lot = ?, id_customer = ?, delivery_place = ?, delivery_term = ?")
            insertCusRec.setInt(1, idLot)
            insertCusRec.setInt(2, idCustomer)
            insertCusRec.setString(3, delivPlace)
            insertCusRec.setString(4, delivTerm)
            insertCusRec.executeUpdate()
            insertCusRec.close()
        }
        val rec = html.selectFirst("td:containsOwn(Требования к участникам:) + td")?.ownText()?.trim { it <= ' ' }
                ?: ""
        if (rec != "") {
            val insertRestr = con.prepareStatement("INSERT INTO ${Prefix}restricts SET id_lot = ?, foreign_info = ?")
            insertRestr.setInt(1, idLot)
            insertRestr.setString(2, rec)
            insertRestr.executeUpdate()
            insertRestr.close()
        }
    }
}