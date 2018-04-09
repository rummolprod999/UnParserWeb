package enterit.tenders

import com.gargoylesoftware.htmlunit.html.HtmlAnchor
import com.gargoylesoftware.htmlunit.html.HtmlDivision
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
        ParserStg.webClient.close()
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
            var idRegion = 0
            if (regionName != "") {
                idRegion = getIdRegion(con, regionName)
            }
            var idTender = 0
            var scoringDateT = page.getFirstByXPath<HtmlSpan>("//td[contains(preceding-sibling::td, 'Дата рассмотрения заявки')]/span")?.textContent?.trim { it <= ' ' }
                    ?: ""
            scoringDateT = scoringDateT.regExpTest("""^(\d{2}\.\d{2}\.\d{4} \d{2}:\d{2})""")
            val scoringDate = scoringDateT.getDateFromString(formatterGpn)
            val insertTender = con.prepareStatement("INSERT INTO ${Prefix}tender SET id_xml = ?, purchase_number = ?, doc_publish_date = ?, href = ?, purchase_object_info = ?, type_fz = ?, id_organizer = ?, id_placing_way = ?, id_etp = ?, end_date = ?, cancel = ?, date_version = ?, num_version = ?, notice_version = ?, xml = ?, print_form = ?, id_region = ?, scoring_date = ?", Statement.RETURN_GENERATED_KEYS)
            insertTender.setString(1, purNum)
            insertTender.setString(2, purNum)
            insertTender.setTimestamp(3, Timestamp(pubDate.time))
            insertTender.setString(4, urlT)
            insertTender.setString(5, purObj)
            insertTender.setInt(6, typeFz)
            insertTender.setInt(7, IdOrganizer)
            insertTender.setInt(8, idPlacingWay)
            insertTender.setInt(9, idEtp)
            insertTender.setTimestamp(10, Timestamp(endDate.time))
            insertTender.setInt(11, cancelstatus)
            insertTender.setTimestamp(12, Timestamp(dateVer.time))
            insertTender.setInt(13, 1)
            insertTender.setString(14, status)
            insertTender.setString(15, urlT)
            insertTender.setString(16, urlT)
            insertTender.setInt(17, idRegion)
            insertTender.setTimestamp(18, Timestamp(scoringDate.time))
            insertTender.executeUpdate()
            val rt = insertTender.generatedKeys
            if (rt.next()) {
                idTender = rt.getInt(1)
            }
            rt.close()
            insertTender.close()
            AddTenderStg++
            val documents = page.getByXPath<HtmlAnchor>("//td[contains(preceding-sibling::td, 'Прочие документы')]//a")
            documents.forEach {
                if (it is HtmlAnchor) {
                    val nameDoc = it.textContent?.trim { it <= ' ' } ?: ""
                    val hrefT = it.getAttribute("href")?.trim { it <= ' ' } ?: ""
                    val href = "$etpUrl$hrefT"
                    if (hrefT != "") {
                        val insertDoc = con.prepareStatement("INSERT INTO ${Prefix}attachment SET id_tender = ?, file_name = ?, url = ?")
                        insertDoc.setInt(1, idTender)
                        insertDoc.setString(2, nameDoc)
                        insertDoc.setString(3, href)
                        insertDoc.executeUpdate()
                        insertDoc.close()
                    }
                }
            }
            val lots = page.getByXPath<HtmlDivision>("//div[contains(@class, 'tradeLotInfo') and contains(@class, 'labelminwidth')]")
            lots.forEach {
                if (it is HtmlDivision) {
                    try {
                        parsingLots(it, con, idTender, page)
                    } catch (e: Exception) {
                        logger("error in ${this::class.simpleName}.parsingLots()", e.stackTrace, e, urlT)
                    }
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

    private fun parsingLots(it: HtmlDivision, con: Connection, idTender: Int, page: HtmlPage) {
        var idLot = 0
        var lotNumT = it.getFirstByXPath<HtmlSpan>(".//span[contains(., 'Лот №')]")?.textContent?.trim { it <= ' ' }
                ?: ""
        lotNumT = lotNumT.regExpTest("""(\d+)${'$'}""")
        val lotNum = Integer.valueOf(lotNumT)
        val currency = it.getFirstByXPath<HtmlSpan>(".//td[contains(preceding-sibling::td, 'Тип валюты')]/span")?.textContent?.trim { it <= ' ' }
                ?: ""
        val nmckT = it.getFirstByXPath<HtmlSpan>(".//td[contains(preceding-sibling::td, 'Начальная/максимальная цена')]/span")?.textContent?.trim { it <= ' ' }
                ?: ""
        val nmck = nmckT.extractPrice()
        val insertLot = con.prepareStatement("INSERT INTO ${Prefix}lot SET id_tender = ?, lot_number = ?, currency = ?, max_price = ?", Statement.RETURN_GENERATED_KEYS)
        insertLot.setInt(1, idTender)
        insertLot.setInt(2, lotNum)
        insertLot.setString(3, currency)
        insertLot.setString(4, nmck)
        insertLot.executeUpdate()
        val rl = insertLot.generatedKeys
        if (rl.next()) {
            idLot = rl.getInt(1)
        }
        rl.close()
        insertLot.close()
        val documents = it.getByXPath<HtmlAnchor>(".//td[contains(preceding-sibling::td, 'Документы лота')]//a")
        documents.forEach {
            if (it is HtmlAnchor) {
                val nameDoc = it.textContent?.trim { it <= ' ' } ?: ""
                val hrefT = it.getAttribute("href")?.trim { it <= ' ' } ?: ""
                val href = "$etpUrl$hrefT"
                if (hrefT != "") {
                    val insertDoc = con.prepareStatement("INSERT INTO ${Prefix}attachment SET id_tender = ?, file_name = ?, url = ?")
                    insertDoc.setInt(1, idTender)
                    insertDoc.setString(2, nameDoc)
                    insertDoc.setString(3, href)
                    insertDoc.executeUpdate()
                    insertDoc.close()
                }
            }
        }
        var idCustomer = 0
        val fullnameOrg = page.getFirstByXPath<HtmlSpan>("//td[contains(preceding-sibling::td, 'Заказчик')]/span")?.textContent?.trim { it <= ' ' }
                ?: ""
        if (fullnameOrg != "") {
            val stmtoc = con.prepareStatement("SELECT id_customer FROM ${Prefix}customer WHERE full_name = ? LIMIT 1")
            stmtoc.setString(1, fullnameOrg)
            val rsoc = stmtoc.executeQuery()
            if (rsoc.next()) {
                idCustomer = rsoc.getInt(1)
                rsoc.close()
                stmtoc.close()
            } else {
                rsoc.close()
                stmtoc.close()
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
        val positions = it.getByXPath<HtmlDivision>(".//div[div[contains(@class, 'box')]/div[@class = 'portlet-title']]")
        if (!positions.isEmpty()) {
            positions.forEach { pr ->
                val purName = pr.getFirstByXPath<HtmlSpan>(".//td[contains(preceding-sibling::td, 'Наименование')]/span")?.textContent?.trim { it <= ' ' }
                        ?: ""
                var quantity = pr.getFirstByXPath<HtmlSpan>(".//td[contains(preceding-sibling::td, 'Количество')]/span")?.textContent?.trim { it <= ' ' }
                        ?: ""
                quantity = quantity.extractPrice()
                val okei = pr.getFirstByXPath<HtmlSpan>(".//td[contains(preceding-sibling::td, 'Ед. измерения')]/span")?.textContent?.trim { it <= ' ' }
                        ?: ""
                val okpd2T = pr.getFirstByXPath<HtmlSpan>(".//td[contains(preceding-sibling::td, 'Код ОКПД 2')]/span")?.textContent?.trim { it <= ' ' }
                        ?: ""
                val okpd2Code = okpd2T.regExpTest("""^([\d\.]+)""")
                val (okpd2GroupCode, okpd2GroupLevel1Code) = getOkpd(okpd2Code)
                val okpd2Name = okpd2T.regExpTest("""^[\d\.]+ - (.+)${'$'}""")
                val insertPurObj = con.prepareStatement("INSERT INTO ${Prefix}purchase_object SET id_lot = ?, id_customer = ?, name = ?, quantity_value = ?, okei = ?, customer_quantity_value = ?, okpd2_code = ?, okpd2_group_code = ?, okpd2_group_level1_code = ?, okpd_name = ?")
                insertPurObj.setInt(1, idLot)
                insertPurObj.setInt(2, idCustomer)
                insertPurObj.setString(3, purName)
                insertPurObj.setString(4, quantity)
                insertPurObj.setString(5, okei)
                insertPurObj.setString(6, quantity)
                insertPurObj.setString(7, okpd2Code)
                insertPurObj.setInt(8, okpd2GroupCode)
                insertPurObj.setString(9, okpd2GroupLevel1Code)
                insertPurObj.setString(10, okpd2Name)
                insertPurObj.executeUpdate()
                insertPurObj.close()
            }
        } else {
            val okpd2T = it.getFirstByXPath<HtmlSpan>(".//td[contains(preceding-sibling::td, 'Код ОКПД 2')]/span")?.textContent?.trim { it <= ' ' }
                    ?: ""
            val okpd2Code = okpd2T.regExpTest("""^([\d\.]+)""")
            val (okpd2GroupCode, okpd2GroupLevel1Code) = getOkpd(okpd2Code)
            val okpd2Name = okpd2T.regExpTest("""^[\d\.]+ - (.+)${'$'}""")
            val quantity = it.getFirstByXPath<HtmlSpan>(".//td[contains(preceding-sibling::td, 'Количество') and not(contains(preceding-sibling::td, 'блокировка'))]/span")?.textContent?.trim { it <= ' ' }
                    ?: ""
            val okei = it.getFirstByXPath<HtmlSpan>(".//td[contains(preceding-sibling::td, 'Ед. измерения')]/span")?.textContent?.trim { it <= ' ' }
                    ?: ""
            val purName = it.getFirstByXPath<HtmlSpan>(".//td[contains(preceding-sibling::td, 'Наименование')]/span")?.textContent?.trim { it <= ' ' }
                    ?: ""
            val insertPurObj = con.prepareStatement("INSERT INTO ${Prefix}purchase_object SET id_lot = ?, id_customer = ?, name = ?, quantity_value = ?, okei = ?, customer_quantity_value = ?, okpd2_code = ?, okpd2_group_code = ?, okpd2_group_level1_code = ?, okpd_name = ?, sum = ?")
            insertPurObj.setInt(1, idLot)
            insertPurObj.setInt(2, idCustomer)
            insertPurObj.setString(3, purName)
            insertPurObj.setString(4, quantity)
            insertPurObj.setString(5, okei)
            insertPurObj.setString(6, quantity)
            insertPurObj.setString(7, okpd2Code)
            insertPurObj.setInt(8, okpd2GroupCode)
            insertPurObj.setString(9, okpd2GroupLevel1Code)
            insertPurObj.setString(10, okpd2Name)
            insertPurObj.setString(11, nmck)
            insertPurObj.executeUpdate()
            insertPurObj.close()
        }
        val delivPlace = it.getFirstByXPath<HtmlSpan>(".//td[contains(preceding-sibling::td, 'Место доставки поставляемых')]/span")?.textContent?.trim { it <= ' ' }
                ?: ""
        if (delivPlace != "") {
            val insertCusRec = con.prepareStatement("INSERT INTO ${Prefix}customer_requirement SET id_lot = ?, id_customer = ?, delivery_place = ?, max_price = ?")
            insertCusRec.setInt(1, idLot)
            insertCusRec.setInt(2, idCustomer)
            insertCusRec.setString(3, delivPlace)
            insertCusRec.setString(4, nmck)
            insertCusRec.executeUpdate()
            insertCusRec.close()
        }
        val rec = it.getFirstByXPath<HtmlSpan>(".//td[contains(preceding-sibling::td, 'Особенности участия субъектов')]/span")?.textContent?.trim { it <= ' ' }
                ?: ""
        if (rec != "") {
            val recContent = "Особенности участия субъектов малого и среднего предпринимательства: $rec"
            val insertRec = con.prepareStatement("INSERT INTO ${Prefix}requirement SET id_lot = ?, content = ?")
            insertRec.setInt(1, idLot)
            insertRec.setString(2, recContent)
            insertRec.executeUpdate()
            insertRec.close()
        }
        val pref = it.getFirstByXPath<HtmlSpan>(".//td[contains(preceding-sibling::td, 'Преференции для контрагентов')]/span")?.textContent?.trim { it <= ' ' }
                ?: ""
        if (pref != "") {
            val prefContent = "Преференции для контрагентов: $pref"
            val insertPref = con.prepareStatement("INSERT INTO ${Prefix}preferense SET id_lot = ?, name = ?")
            insertPref.setInt(1, idLot)
            insertPref.setString(2, prefContent)
            insertPref.executeUpdate()
            insertPref.close()
        }

    }
}