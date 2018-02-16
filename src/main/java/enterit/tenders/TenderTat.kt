package enterit.tenders

import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.DomText
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.gargoylesoftware.htmlunit.html.HtmlTableCell
import com.gargoylesoftware.htmlunit.html.HtmlTableRow
import enterit.*
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.sql.Timestamp
import java.util.*

class TenderTat(val status: String, var purNum: String, val purObj: String, val nmck: String, val datePub: Date, val dateEnd: Date, val url: String, val fullnameOrg: String, val currency: String, val webClient: WebClient) {
    companion object O {
        const val typeFz = 16
    }

    fun parsing() {
        if (purNum == "") {
            logger("Empty purchase number in $url")
            return
        }
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
            val startDate = datePub
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
            var page: HtmlPage? = null
            if (url != "") {
                webClient.waitForBackgroundJavaScript(2000)
                page = webClient.getPage(url)


            }
            var contactP = ""
            if (page != null) {
                val td = page.getByXPath<DomText>("//td[b = 'Контактное лицо заказчика: ']/text()")
                if (!td.isEmpty()) {
                    contactP = td[0].textContent.trim { it <= ' ' }
                }
            }
            var IdOrganizer = 0
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

                    val stmtins = con.prepareStatement("INSERT INTO ${Prefix}organizer SET full_name = ?, contact_person = ?", Statement.RETURN_GENERATED_KEYS)
                    stmtins.setString(1, fullnameOrg)
                    stmtins.setString(2, contactP)
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
            val etpName = "ПАО \"Татнефть\""
            val etpUrl = "https://etp.tatneft.ru"
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
            var placingWay = ""
            if (page != null) {
                val td = page.getByXPath<DomText>("//td[b = 'Тип торгов: ']/text()")
                if (!td.isEmpty()) {
                    placingWay = td[0].textContent.trim { it <= ' ' }
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
            var idTender = 0
            val insertTender = con.prepareStatement("INSERT INTO ${Prefix}tender SET id_region = 0, id_xml = ?, purchase_number = ?, doc_publish_date = ?, href = ?, purchase_object_info = ?, type_fz = ?, id_organizer = ?, id_placing_way = ?, id_etp = ?, end_date = ?, cancel = ?, date_version = ?, num_version = ?, xml = ?, print_form = ?, notice_version = ?", Statement.RETURN_GENERATED_KEYS)
            insertTender.setString(1, purNum)
            insertTender.setString(2, purNum)
            insertTender.setTimestamp(3, Timestamp(startDate.time))
            insertTender.setString(4, url)
            insertTender.setString(5, purObj)
            insertTender.setInt(6, typeFz)
            insertTender.setInt(7, IdOrganizer)
            insertTender.setInt(8, IdPlacingWay)
            insertTender.setInt(9, IdEtp)
            insertTender.setTimestamp(10, Timestamp(dateEnd.time))
            insertTender.setInt(11, cancelstatus)
            insertTender.setTimestamp(12, Timestamp(startDate.time))
            insertTender.setInt(13, 1)
            insertTender.setString(14, url)
            insertTender.setString(15, url)
            insertTender.setString(16, status)
            insertTender.executeUpdate()
            val rt = insertTender.generatedKeys
            if (rt.next()) {
                idTender = rt.getInt(1)
            }
            rt.close()
            insertTender.close()
            AddTenderTat++
            if (page != null) {
                for (d in listOf("Приглашение", "Спецификация", "Заявка на формирование лота", "Договор", "ТТУ", "Гарантийное письмо", "Краткая анкета поставщика ", "Условия поставки", "Проект типового договора", "Чертеж")) {
                    val doc = page.getByXPath<HtmlTableCell>("//td[@colspan and preceding-sibling::td = '$d']")
                    if (!doc.isEmpty()) {
                        val docs = doc[0]
                        var nameDoc = ""
                        var href = ""
                        val nameDocT = docs.getElementsByTagName("a")
                        if (!nameDocT.isEmpty()) {
                            nameDoc = nameDocT[0].textContent.trim { it <= ' ' }
                        }
                        var hrefT = nameDocT[0]?.getAttribute("href") ?: ""
                        if (hrefT != "") {
                            hrefT = hrefT.replace("javascript:apxDownloadFile(", "").replace(")", "")
                            val hrefA = hrefT.split(",")
                            if (hrefA.size == 3) href = "https://etp.tatneft.ru/pls/tzp/wwv_flow.show?p_flow_id=220&p_flow_step_id=2155&p_instance=13772509579946&p_request=APPLICATION_PROCESS=APX_CARD_FILE_DOWNLOAD&x01=${hrefA[0]}&x02=${hrefA[1]}&x03=${hrefA[2]}"
                        }
                        if (href != "") {
                            val insertDoc = con.prepareStatement("INSERT INTO ${Prefix}attachment SET id_tender = ?, file_name = ?, url = ?, description = ?")
                            insertDoc.setInt(1, idTender)
                            insertDoc.setString(2, nameDoc)
                            insertDoc.setString(3, href)
                            insertDoc.setString(4, d)
                            insertDoc.executeUpdate()
                            insertDoc.close()
                        }
                    }

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
            if (page != null) {
                try {
                    getPo(page, con, idLot, idCustomer)
                } catch (e: Exception) {
                    logger("Error in getPo function", e.stackTrace, e)
                }
                try {
                    getCusPr(page, con, idLot, idCustomer)
                } catch (e: Exception) {
                    logger("Error in getCusPr function", e.stackTrace, e)
                }
                try {
                    getRest(page, con, idLot)
                } catch (e: Exception) {
                    logger("Error in getRest function", e.stackTrace, e)
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

    private fun getRest(page: HtmlPage, con: Connection, idLot: Int) {
        for (d in listOf("Требование к качеству", "Требования к участникам торгов")) {
            val resT = page.getByXPath<HtmlTableRow>("//tr[td[div = '$d']]")
            var r1 = ""
            if (!resT.isEmpty()) {
                r1 = resT[0].getCell(1).textContent.trim { it <= ' ' }
                val insertRestr = con.prepareStatement("INSERT INTO ${Prefix}restricts SET id_lot = ?, foreign_info = ?, info = ?")
                insertRestr.setInt(1, idLot)
                insertRestr.setString(2, d)
                insertRestr.setString(3, r1)
                insertRestr.executeUpdate()
                insertRestr.close()
            }
        }
    }

    private fun getCusPr(page: HtmlPage, con: Connection, idLot: Int, idCustomer: Int) {
        var delivPlace = ""
        var delivTerm1 = ""
        var delivTerm2 = ""
        val delivPlaceT = page.getByXPath<HtmlTableRow>("//tr[td[div = 'Место поставки']]")
        if (!delivPlaceT.isEmpty()) {
            delivPlace = delivPlaceT[0].getCell(1).textContent.trim { it <= ' ' }
        }
        val delivT1 = page.getByXPath<HtmlTableRow>("//tr[td[div = 'Условия и сроки поставки']]")
        if (!delivT1.isEmpty()) {
            delivTerm1 = delivT1[0].getCell(1).textContent.trim { it <= ' ' }
        }
        val delivT2 = page.getByXPath<HtmlTableRow>("//tr[td[div = 'Условия и сроки оплаты']]")
        if (!delivT2.isEmpty()) {
            delivTerm2 = delivT2[0].getCell(1).textContent.trim { it <= ' ' }
        }
        val delivTerm = "$delivTerm1 $delivTerm2".trim { it <= ' ' }
        if (delivPlace != "") {
            val insertCusRec = con.prepareStatement("INSERT INTO ${Prefix}customer_requirement SET id_lot = ?, id_customer = ?, delivery_term = ?, delivery_place = ?, max_price = ?")
            insertCusRec.setInt(1, idLot)
            insertCusRec.setInt(2, idCustomer)
            insertCusRec.setString(3, delivTerm)
            insertCusRec.setString(4, delivPlace)
            insertCusRec.setString(5, nmck)
            insertCusRec.executeUpdate()
            insertCusRec.close()
        }
    }

    private fun getPo(page: HtmlPage, con: Connection, idLot: Int, idCustomer: Int) {
        val pur = page.getByXPath<HtmlTableRow>("//table[@class = 'ReportTbl']/tbody[2]/tr")
        if (!pur.isEmpty()) {
            for (p in pur) {
                var purName = p.getCell(1)?.textContent?.trim { it <= ' ' } ?: ""
                val quant = p.getCell(2)?.textContent?.trim { it <= ' ' } ?: ""
                val okei = p.getCell(3)?.textContent?.trim { it <= ' ' } ?: ""
                var addInfo = ""
                try {
                    addInfo = p.getCell(4)?.textContent?.trim { it <= ' ' } ?: ""
                } catch (e: Exception) {
                }
                if (addInfo != "") purName = "$purName $addInfo"
                val insertPurObj = con.prepareStatement("INSERT INTO ${Prefix}purchase_object SET id_lot = ?, id_customer = ?, name = ?, quantity_value = ?, okei = ?, customer_quantity_value = ?")
                insertPurObj.setInt(1, idLot)
                insertPurObj.setInt(2, idCustomer)
                insertPurObj.setString(3, purName)
                insertPurObj.setString(4, quant)
                insertPurObj.setString(5, okei)
                insertPurObj.setString(6, quant)
                insertPurObj.executeUpdate()
                insertPurObj.close()
            }

        }
    }
}