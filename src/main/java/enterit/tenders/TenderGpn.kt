package enterit.tenders

import enterit.*
import enterit.parsers.ParserGpn
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.sql.Timestamp
import java.util.*

class TenderGpn(val status: String, val url: String) {
    companion object TypeFz {
        val typeFz = 13
    }

    fun parsing() {
        val stPage = downloadFromUrl(url)
        if (stPage == "") {
            logger("Gets empty string TenderGpn", url)
            return
        }
        val html = Jsoup.parse(stPage)
        val uTC = html.selectFirst("div:contains(Местное время:) + div")?.ownText()?.trim { it <= ' ' } ?: ""
        val offset = getOffset(uTC)
        val startDateT = html.selectFirst("div:contains(Дата начала приема:) + div")?.ownText()?.replace(",", "")?.trim { it <= ' ' }
                ?: ""
        val endDateT = html.selectFirst("div:contains(Дата окончания приема:) + div")?.ownText()?.replace(",", "")?.trim { it <= ' ' }
                ?: ""
        val startDate = getDateFromFormatOffset(startDateT, formatterGpn, offset)
        val endDate = getDateFromFormatOffset(endDateT, formatterGpn, offset)
        if (startDate == Date(0L)) {
            logger("Empty start date in $url")
            return
        }
        val purNum = html.selectFirst("div.info-number span")?.ownText()?.replace("№", "")?.trim { it <= ' ' } ?: ""
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
            var update = false
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
            val fullnameOrg = html.selectFirst("div:contains(Заказчик:) + div")?.ownText()?.trim { it <= ' ' } ?: ""
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
                    val phone = html.selectFirst("div:contains(Контактная информация:) + div > div:contains(Тел:)")?.text()?.trim { it <= ' ' }
                            ?: ""
                    val email = html.selectFirst("div:contains(Контактная информация:) + div > div:contains(E-mail:)")?.text()?.trim { it <= ' ' }
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
            var IdEtp = 0
            val etpName = "ГАЗПРОМ НЕФТЬ"
            val etpUrl = "http://zakupki.gazprom-neft.ru/"
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
            val placingWay = html.selectFirst("div:contains(Вид процедуры:) + div")?.text()?.trim { it <= ' ' }
                    ?: ""
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
            val purObj = html.selectFirst("div.info-title")?.ownText()?.trim { it <= ' ' }
                    ?: ""
            var idTender = 0
            val scoringDT = html.selectFirst("div:contains(Дата вскрытия:) + div")?.ownText()?.trim { it <= ' ' } ?: ""
            val scoringDate = getDateFromFormat(scoringDT, formatterGpn)
            val insertTender = con.prepareStatement("INSERT INTO ${Prefix}tender SET id_region = 0, id_xml = ?, purchase_number = ?, doc_publish_date = ?, href = ?, purchase_object_info = ?, type_fz = ?, id_organizer = ?, id_placing_way = ?, id_etp = ?, end_date = ?, cancel = ?, date_version = ?, num_version = ?, notice_version = ?, xml = ?, print_form = ?, scoring_date = ?", Statement.RETURN_GENERATED_KEYS)
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
            insertTender.setString(14, status)
            insertTender.setString(15, url)
            insertTender.setString(16, url)
            insertTender.setTimestamp(17, Timestamp(scoringDate.time))
            insertTender.executeUpdate()
            val rt = insertTender.generatedKeys
            if (rt.next()) {
                idTender = rt.getInt(1)
            }
            rt.close()
            insertTender.close()
            if (update) {
                UpTenderGpn++
            } else {
                AddTenderGpn++
            }
            val documents: Elements = html.select("table#files tbody tr")
            documents.forEach { doc ->
                val hrefT = doc.select("tr > td > a[href]")?.attr("href")?.trim { it <= ' ' } ?: ""
                val href = "${ParserGpn.BaseT}$hrefT"
                val nameDoc = doc.select("tr > td > a[href]")?.text()?.trim { it <= ' ' } ?: ""
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
            val lots: Elements = html.select("div.purchase-lots table tbody tr")
            lots.forEach { lot ->
                val noLots = lot.selectFirst("tr > td:contains(лотов нет)")
                if (noLots != null) return@forEach
                val lotNumT = lot.selectFirst("tr > td:eq(0)")?.text()?.trim { it <= ' ' } ?: ""
                val lotNum = if (lotNumT.tryParseInt()) Integer.parseInt(lotNumT) else 1
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
                if (fullnameOrg != "") {
                    val stmto = con.prepareStatement("SELECT id_customer FROM ${Prefix}customer WHERE full_name = ? LIMIT 1")
                    stmto.setString(1, fullnameOrg)
                    val rso = stmto.executeQuery()
                    if (rso.next()) {
                        idCustomer = rso.getInt(1)
                        rso.close()
                        stmto.close()
                    } else {
                        rso.close()
                        stmto.close()
                        val stmtins = con.prepareStatement("INSERT INTO ${Prefix}customer SET full_name = ?, is223=1, reg_num = ?", Statement.RETURN_GENERATED_KEYS)
                        stmtins.setString(1, fullnameOrg)
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
                val lotName = lot.selectFirst("tr > td:eq(1)")?.text()?.trim { it <= ' ' } ?: ""
                val lotObj = lot.selectFirst("tr > td:eq(4)")?.text()?.trim { it <= ' ' } ?: ""
                val quantityValue = lot.selectFirst("tr > td:eq(3)")?.text()?.trim { it <= ' ' } ?: ""
                val purName = "$lotName $lotObj"
                val insertPurObj = con.prepareStatement("INSERT INTO ${Prefix}purchase_object SET id_lot = ?, id_customer = ?, name = ?, quantity_value = ?, customer_quantity_value = ?")
                insertPurObj.setInt(1, idLot)
                insertPurObj.setInt(2, idCustomer)
                insertPurObj.setString(3, purName)
                insertPurObj.setString(4, quantityValue)
                insertPurObj.setString(5, quantityValue)
                insertPurObj.executeUpdate()
                insertPurObj.close()
                val delivPlace = html.selectFirst("div:contains(Место оказания работ/услуг:) + div")?.ownText()?.trim { it <= ' ' }
                        ?: ""
                val delivTermT = lot.selectFirst("tr > td:eq(2)")?.text()?.trim { it <= ' ' } ?: ""
                val delivTerm = "Срок выполнения: $delivTermT"
                if (delivPlace != "") {
                    val insertCusRec = con.prepareStatement("INSERT INTO ${Prefix}customer_requirement SET id_lot = ?, id_customer = ?, delivery_term = ?, delivery_place = ?")
                    insertCusRec.setInt(1, idLot)
                    insertCusRec.setInt(2, idCustomer)
                    insertCusRec.setString(3, delivTerm)
                    insertCusRec.setString(4, delivPlace)
                    insertCusRec.executeUpdate()
                    insertCusRec.close()
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
}