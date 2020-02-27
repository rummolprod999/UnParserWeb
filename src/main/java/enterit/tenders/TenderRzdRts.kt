package enterit.tenders

import enterit.*
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.sql.Timestamp

class TenderRzdRts(val urlTend: String, val purNum: String, val placingWay: String, val applGuarantee: String, val currency: String, val contrGuarantee: String, val nmck: String, val status: String) {
    companion object O {
        const val typeFz = 144
    }

    fun parsing() {
        if (purNum == "") {
            logger("Empty purchase number in $urlTend")
            return
        }
        val stPage = downloadFromUrl(urlTend)
        if (stPage == "") {
            logger("Gets empty string TenderEtpRf", urlTend)
            return
        }
        val html = Jsoup.parse(stPage)
        var startDateT = html.selectFirst("td:containsOwn(Дата публикации) + td")?.ownText()?.trim() ?: ""
        if (startDateT == "") {
            startDateT = html.selectFirst("td:containsOwn(Дата публикации) + td > span")?.ownText()?.trim() ?: ""
        }
        val datePub = getDateFromFormat(startDateT, formatterGpn)
        var endDateT = html.selectFirst("td:containsOwn(Дата окончания подачи заявок) + td > span")?.ownText()?.trim()
                ?: ""
        if (endDateT == "") {
            endDateT = html.selectFirst("td:containsOwn(Дата окончания подачи заявок) + td")?.ownText()?.trim() ?: ""
        }
        val dateEnd = getDateFromFormat(endDateT, formatterGpn)
        val status = html.selectFirst("td:containsOwn(Статус на площадке) + td")?.ownText()?.trim()
                ?: ""
        DriverManager.getConnection(UrlConnect, UserDb, PassDb).use(fun(con: Connection) {
            val stmt0 = con.prepareStatement("SELECT id_tender FROM ${Prefix}tender WHERE purchase_number = ? AND date_version = ? AND type_fz = ? AND notice_version = ? AND end_date = ?")
            stmt0.setString(1, purNum)
            stmt0.setTimestamp(2, Timestamp(datePub.time))
            stmt0.setInt(3, typeFz)
            stmt0.setString(4, status)
            stmt0.setTimestamp(5, Timestamp(dateEnd.time))
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
            val startDate = datePub
            val stmt = con.prepareStatement("SELECT id_tender, date_version FROM ${Prefix}tender WHERE purchase_number = ? AND cancel=0 AND type_fz = ?")
            stmt.setString(1, purNum)
            stmt.setInt(2, typeFz)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                update = true
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
            val fullnameOrg = html.selectFirst("td:containsOwn(Организатор) + td a span")?.ownText()?.trim()
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
            val etpName = "РТС-тендер секция РЖД"
            val etpUrl = "https://rzd.rts-tender.ru"
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
            var idReg = 0
            val reg = html.selectFirst("td:containsOwn(Регион) + td a span")?.ownText()?.trim()
                    ?: ""
            if (reg != "") {
                val re = getRegion(reg)
                if (re != "") {
                    val stmto = con.prepareStatement("SELECT id FROM region WHERE name LIKE ?")
                    stmto.setString(1, "%$re%")
                    val rso = stmto.executeQuery()
                    if (rso.next()) {
                        idReg = rso.getInt(1)
                        rso.close()
                        stmto.close()
                    } else {
                        rso.close()
                        stmto.close()
                    }
                }
            }
            val purObj = html.selectFirst("td:containsOwn(Наименование) + td")?.ownText()?.trim()
                    ?: ""
            var idTender = 0
            val insertTender = con.prepareStatement("INSERT INTO ${Prefix}tender SET id_xml = ?, purchase_number = ?, doc_publish_date = ?, href = ?, purchase_object_info = ?, type_fz = ?, id_organizer = ?, id_placing_way = ?, id_etp = ?, end_date = ?, cancel = ?, date_version = ?, num_version = ?, notice_version = ?, xml = ?, print_form = ?, id_region = ?", Statement.RETURN_GENERATED_KEYS)
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
            insertTender.setString(14, status)
            insertTender.setString(15, urlTend)
            insertTender.setString(16, urlTend)
            insertTender.setInt(17, idReg)
            insertTender.executeUpdate()
            val rt = insertTender.generatedKeys
            if (rt.next()) {
                idTender = rt.getInt(1)
            }
            rt.close()
            insertTender.close()
            if (update) {
                UpTenderRtsRzd++
            } else {
                AddTenderRtsRzd++
            }
            val documents: Elements = html.select("div:containsOwn(Документы) + div table.table-carts2 tbody tr")
            documents.forEach { doc ->
                var href = doc.select("td a[href]")?.attr("href")?.trim { it <= ' ' } ?: ""
                if (href == "") {
                    href = doc.select("td a[href] span")?.attr("href")?.trim { it <= ' ' } ?: ""
                }
                val nameDoc = doc.select("td:eq(0)")?.text()?.trim { it <= ' ' } ?: ""
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
            val LotNumber = 1
            val insertLot = con.prepareStatement("INSERT INTO ${Prefix}lot SET id_tender = ?, lot_number = ?, currency = ?, max_price = ?", Statement.RETURN_GENERATED_KEYS)
            insertLot.setInt(1, idTender)
            insertLot.setInt(2, LotNumber)
            insertLot.setString(3, currency)
            insertLot.setString(4, nmck)
            insertLot.executeUpdate()
            val rl = insertLot.generatedKeys
            if (rl.next()) {
                idLot = rl.getInt(1)
            }
            rl.close()
            insertLot.close()
            var idCustomer = 0
            var fullnameCus = html.selectFirst("td:containsOwn(Заказчик) + td a span")?.ownText()?.trim()
                    ?: ""
            if (fullnameCus == "") {
                fullnameCus = html.selectFirst("td:containsOwn(Заказчик) + td span")?.text()?.trim()
                        ?: ""
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
            val purchObj: Elements = html.select("div:containsOwn(Информация об объектах) + div table.table-carts tbody tr")
            purchObj.forEach { po ->
                val okpd2Code = po.select("td:eq(1)")?.text()?.trim { it <= ' ' } ?: ""
                val namePO = po.select("td:eq(0)")?.text()?.trim { it <= ' ' } ?: ""
                val (okpd2GroupCode, okpd2GroupLevel1Code) = getOkpd(okpd2Code)
                val okei = po.select("td:eq(2)")?.text()?.trim { it <= ' ' } ?: ""
                val quantityValue = po.select("td:eq(3)")?.text()?.trim { it <= ' ' } ?: ""
                val priceT = po.select("td:eq(4)")?.text()?.trim { it <= ' ' } ?: ""
                val price = returnPriceEtpRf(priceT)
                val insertPurObj = con.prepareStatement("INSERT INTO ${Prefix}purchase_object SET id_lot = ?, id_customer = ?, name = ?, quantity_value = ?, okei = ?, customer_quantity_value = ?, okpd2_code = ?, okpd2_group_code = ?, okpd2_group_level1_code = ?, price = ?")
                insertPurObj.setInt(1, idLot)
                insertPurObj.setInt(2, idCustomer)
                insertPurObj.setString(3, namePO)
                insertPurObj.setString(4, quantityValue)
                insertPurObj.setString(5, okei)
                insertPurObj.setString(6, quantityValue)
                insertPurObj.setString(7, okpd2Code)
                insertPurObj.setInt(8, okpd2GroupCode)
                insertPurObj.setString(9, okpd2GroupLevel1Code)
                insertPurObj.setString(10, price)
                insertPurObj.executeUpdate()
                insertPurObj.close()
            }
            val delivPlace = html.selectFirst("td:containsOwn(Место поставки) + td div")?.text()?.trim()
                    ?: ""
            if (delivPlace != "") {
                val insertCusRec = con.prepareStatement("INSERT INTO ${Prefix}customer_requirement SET id_lot = ?, id_customer = ?, delivery_place = ?, application_guarantee_amount = ?, max_price = ?, contract_guarantee_amount = ?")
                insertCusRec.setInt(1, idLot)
                insertCusRec.setInt(2, idCustomer)
                insertCusRec.setString(3, delivPlace)
                insertCusRec.setString(4, applGuarantee)
                insertCusRec.setString(5, nmck)
                insertCusRec.setString(6, contrGuarantee)
                insertCusRec.executeUpdate()
                insertCusRec.close()
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