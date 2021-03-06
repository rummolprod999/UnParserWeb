package enterit.tenders

import enterit.*
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.sql.Timestamp
import java.util.*

class TenderEtpRf(
    val status: String,
    val entNum: String,
    var purNum: String,
    val purObj: String,
    val nmck: String,
    var placingWay: String,
    val datePub: Date,
    val dateEnd: Date,
    val url: String
) {
    companion object TypeFz {
        val typeFz = 12
    }

    fun parsing() {
        if (purNum == "" || purNum == " ") {
            val tp = url.split('/')
            if (tp.count() > 0) {
                purNum = tp.last()
            }

        }
        if (purNum == "") {
            logger("Empty $purNum", url)
            return
        }
        DriverManager.getConnection(UrlConnect, UserDb, PassDb).use(fun(con: Connection) {
            val stmt0 =
                con.prepareStatement("SELECT id_tender FROM ${Prefix}tender WHERE purchase_number = ? AND date_version = ? AND type_fz = ? AND notice_version = ?")
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
            val stPage = downloadFromUrlEtpRf(url, i = 2)
            if (stPage == "") {
                logger("Gets empty string TenderEtpRf", url)
                return
            }
            val html = Jsoup.parse(stPage)
            val eis = html.selectFirst("td:containsOwn(Состояние извещения) ~ td")?.ownText()?.trim() ?: ""
            if (eis == "Опубликовано в ЕИС") {
                logger("Опубликовано в ЕИС")
                //return
            }
            var cancelstatus = 0
            var update = false
            val stmt =
                con.prepareStatement("SELECT id_tender, date_version FROM ${Prefix}tender WHERE purchase_number = ? AND cancel=0 AND type_fz = ?")
            stmt.setString(1, purNum)
            stmt.setInt(2, typeFz)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                update = true
                val idT = rs.getInt(1)
                val dateB: Timestamp = rs.getTimestamp(2)
                if (datePub.after(dateB) || dateB == Timestamp(datePub.time)) {
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
            var fullNameOrg = ""
            val innOrg = html.selectFirst("td:containsOwn(ИНН) ~ td")?.ownText()?.trim() ?: ""
            val kppOrg = html.selectFirst("td:containsOwn(КПП) ~ td")?.ownText()?.trim() ?: ""
            if (innOrg != "") {
                val stmto =
                    con.prepareStatement("SELECT id_organizer FROM ${Prefix}organizer WHERE inn = ? AND kpp = ?")
                stmto.setString(1, innOrg)
                stmto.setString(2, kppOrg)
                val rso = stmto.executeQuery()
                if (rso.next()) {
                    IdOrganizer = rso.getInt(1)
                    rso.close()
                    stmto.close()
                } else {
                    rso.close()
                    stmto.close()
                    fullNameOrg =
                        html.selectFirst("td:containsOwn(Наименование организации, размещающей заказ) ~ td")?.ownText()
                            ?.trim()
                            ?: ""
                    val postalAdr =
                        html.selectFirst("td:containsOwn(Почтовый адрес организации) ~ td")?.ownText()?.trim()
                            ?: ""
                    val email =
                        html.selectFirst("td:containsOwn(e-mail адрес контактного лица) ~ td")?.ownText()?.trim()
                            ?: ""
                    val phone = html.selectFirst("td:containsOwn(Телефон контактного лица) ~ td")?.ownText()?.trim()
                        ?: ""
                    val fax = html.selectFirst("td:containsOwn(Факс контактного лица) ~ td")?.ownText()?.trim()
                        ?: ""
                    val contactPerson = html.selectFirst("td:containsOwn(Контактное лицо) ~ td")?.ownText()?.trim()
                        ?: ""
                    val stmtins = con.prepareStatement(
                        "INSERT INTO ${Prefix}organizer SET full_name = ?, inn = ?, kpp = ?, post_address = ?, contact_person = ?, contact_email = ?, contact_phone = ?, contact_fax = ?",
                        Statement.RETURN_GENERATED_KEYS
                    )
                    stmtins.setString(1, fullNameOrg)
                    stmtins.setString(2, innOrg)
                    stmtins.setString(3, kppOrg)
                    stmtins.setString(4, postalAdr)
                    stmtins.setString(5, contactPerson)
                    stmtins.setString(6, email)
                    stmtins.setString(7, phone)
                    stmtins.setString(8, fax)
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
            val etpName = "ETPRF.RU"
            val etpUrl = "http://etprf.ru/"
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
            if (placingWay == "") {
                placingWay = html.selectFirst("td:containsOwn(Способ закупки) ~ td")?.ownText()?.trim() ?: ""
            }
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
            val printForm = url.replace("/id/", "/Print/id/")
            var idTender = 0
            var scoringDT =
                html.selectFirst("td:containsOwn(Дата и время рассмотрения заявок) ~ td")?.ownText()?.trim { it <= ' ' }
                    ?: ""
            var scoringDate = getDateFromFormat(scoringDT, formatterEtpRf)
            if (scoringDate == Date(0L)) {
                scoringDT = html.selectFirst("td:containsOwn(Дата и время рассмотрения заявок) ~ td div")?.ownText()
                    ?.trim { it <= ' ' }
                    ?: ""
                scoringDate = getDateFromFormat(scoringDT, formatterEtpRf)
            }

            val insertTender = con.prepareStatement(
                "INSERT INTO ${Prefix}tender SET id_region = 0, id_xml = ?, purchase_number = ?, doc_publish_date = ?, href = ?, purchase_object_info = ?, type_fz = ?, id_organizer = ?, id_placing_way = ?, id_etp = ?, end_date = ?, cancel = ?, date_version = ?, num_version = ?, notice_version = ?, xml = ?, print_form = ?, scoring_date = ?",
                Statement.RETURN_GENERATED_KEYS
            )
            insertTender.setString(1, entNum)
            insertTender.setString(2, purNum)
            insertTender.setTimestamp(3, Timestamp(datePub.time))
            insertTender.setString(4, url)
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
            insertTender.setString(15, url)
            insertTender.setString(16, printForm)
            insertTender.setTimestamp(17, Timestamp(scoringDate.time))
            insertTender.executeUpdate()
            val rt = insertTender.generatedKeys
            if (rt.next()) {
                idTender = rt.getInt(1)
            }
            rt.close()
            insertTender.close()
            if (update) {
                UpTenderEtpRf++
            } else {
                AddTenderEtpRf++
            }
            val documents: Elements = html.select("table[data-orm-table-id = DocumentMetas] tbody tr[style]")
            if (documents.count() > 0) {
                documents.forEach { doc ->
                    val href = doc.select("td:eq(0) > a[href]")?.attr("href")?.trim { it <= ' ' } ?: ""
                    val descDoc = doc.select("td:eq(1)").text().trim { it <= ' ' }
                    val nameDoc = doc.select("td:eq(0) > a[href]")?.text()?.trim { it <= ' ' } ?: ""
                    if (href != "") {
                        val insertDoc =
                            con.prepareStatement("INSERT INTO ${Prefix}attachment SET id_tender = ?, file_name = ?, url = ?, description = ?")
                        insertDoc.setInt(1, idTender)
                        insertDoc.setString(2, descDoc)
                        insertDoc.setString(3, href)
                        insertDoc.setString(4, nameDoc)
                        insertDoc.executeUpdate()
                        insertDoc.close()
                    }
                }
            }
            var idLot = 0
            val LotNumber = 1
            val priceLot = returnPriceEtpRf(nmck)
            val currency = html.selectFirst("td:containsOwn(Валюта) ~ td")?.ownText()?.trim { it <= ' ' } ?: ""
            val insertLot = con.prepareStatement(
                "INSERT INTO ${Prefix}lot SET id_tender = ?, lot_number = ?, currency = ?, max_price = ?",
                Statement.RETURN_GENERATED_KEYS
            )
            insertLot.setInt(1, idTender)
            insertLot.setInt(2, LotNumber)
            insertLot.setString(3, currency)
            insertLot.setString(4, priceLot)
            insertLot.executeUpdate()
            val rl = insertLot.generatedKeys
            if (rl.next()) {
                idLot = rl.getInt(1)
            }
            rl.close()
            insertLot.close()
            var idCustomer = 0
            if (innOrg != "") {
                val stmto = con.prepareStatement("SELECT id_customer FROM ${Prefix}customer WHERE inn = ? LIMIT 1")
                stmto.setString(1, innOrg)
                val rso = stmto.executeQuery()
                if (rso.next()) {
                    idCustomer = rso.getInt(1)
                    rso.close()
                    stmto.close()
                } else {
                    rso.close()
                    stmto.close()
                    val stmtins = con.prepareStatement(
                        "INSERT INTO ${Prefix}customer SET full_name = ?, is223=1, reg_num = ?, inn = ?",
                        Statement.RETURN_GENERATED_KEYS
                    )
                    stmtins.setString(1, fullNameOrg)
                    stmtins.setString(2, java.util.UUID.randomUUID().toString())
                    stmtins.setString(3, innOrg)
                    stmtins.executeUpdate()
                    val rsoi = stmtins.generatedKeys
                    if (rsoi.next()) {
                        idCustomer = rsoi.getInt(1)
                    }
                    rsoi.close()
                    stmtins.close()
                }
            }
            val purObj: Elements = html.select("table[data-orm-table-id = LotItems] tbody tr[style]")
            if (purObj.count() > 0) {
                purObj.forEach { po ->
                    val okpd2Code = po.select("td:eq(1)")?.text()?.trim { it <= ' ' } ?: ""
                    val okpd2Name = po.select("td:eq(2)")?.text()?.trim { it <= ' ' } ?: ""
                    val (okpd2GroupCode, okpd2GroupLevel1Code) = getOkpd(okpd2Code)
                    val okei = po.select("td:eq(7)")?.text()?.trim { it <= ' ' } ?: ""
                    val quantityValue = po.select("td:eq(8)")?.text()?.trim { it <= ' ' } ?: ""
                    var namePO =
                        html.selectFirst("td:containsOwn(Полное наименование (предмет договора)) ~ td")?.ownText()
                            ?.trim { it <= ' ' }
                            ?: ""
                    if (namePO == "") {
                        namePO =
                            html.selectFirst("td:containsOwn(Предмет договора) ~ td")?.ownText()?.trim { it <= ' ' }
                                ?: ""
                    }
                    if (namePO == "") {
                        namePO = okpd2Name
                    }
                    val insertPurObj =
                        con.prepareStatement("INSERT INTO ${Prefix}purchase_object SET id_lot = ?, id_customer = ?, name = ?, quantity_value = ?, okei = ?, customer_quantity_value = ?, okpd2_code = ?, okpd2_group_code = ?, okpd2_group_level1_code = ?")
                    insertPurObj.setInt(1, idLot)
                    insertPurObj.setInt(2, idCustomer)
                    insertPurObj.setString(3, namePO)
                    insertPurObj.setString(4, quantityValue)
                    insertPurObj.setString(5, okei)
                    insertPurObj.setString(6, quantityValue)
                    insertPurObj.setString(7, okpd2Code)
                    insertPurObj.setInt(8, okpd2GroupCode)
                    insertPurObj.setString(9, okpd2GroupLevel1Code)
                    insertPurObj.executeUpdate()
                    insertPurObj.close()
                }
            }
            val delivPlace =
                html.selectFirst("td:containsOwn(Место поставки, выполнения работ, оказания услуг) ~ td")?.ownText()
                    ?.trim { it <= ' ' }
                    ?: ""
            val delivTerm =
                html.selectFirst("td:containsOwn(Дополнительные комментарии) ~ td")?.ownText()?.trim { it <= ' ' }
                    ?: ""
            var applGAmount =
                html.selectFirst("td:containsOwn(Размер обеспечения(резервирования оплаты) заявки на участие, в рублях) ~ td")
                    ?.ownText()?.trim { it <= ' ' }
                    ?: ""
            applGAmount = applGAmount.replace("руб.", "")
            val applGuaranteeAmount = returnPriceEtpRf(applGAmount)
            if (delivPlace != "") {
                val insertCusRec =
                    con.prepareStatement("INSERT INTO ${Prefix}customer_requirement SET id_lot = ?, id_customer = ?, delivery_term = ?, delivery_place = ?, application_guarantee_amount = ?, max_price = ?")
                insertCusRec.setInt(1, idLot)
                insertCusRec.setInt(2, idCustomer)
                insertCusRec.setString(3, delivTerm)
                insertCusRec.setString(4, delivPlace)
                insertCusRec.setString(5, applGuaranteeAmount)
                insertCusRec.setString(6, priceLot)
                insertCusRec.executeUpdate()
                insertCusRec.close()
            }
            val restr =
                html.selectFirst("td:containsOwn(Требование к отсутствию участника в реестре недобросовестных поставщиков) ~ td")
                    ?.ownText()?.trim { it <= ' ' }
                    ?: ""
            if (restr == "Да") {
                val restInfo = "Требование к отсутствию участника в реестре недобросовестных поставщиков"
                val insertRestr = con.prepareStatement("INSERT INTO ${Prefix}restricts SET id_lot = ?, info = ?")
                insertRestr.setInt(1, idLot)
                insertRestr.setString(2, restInfo)
                insertRestr.executeUpdate()
                insertRestr.close()
            }
            val msp = html.selectFirst("td:containsOwn(Торги для субъектов малого и среднего предпринимательства) ~ td")
                ?.ownText()?.trim { it <= ' ' }
                ?: ""
            if (msp == "Да") {
                val recContent = "Торги для субъектов малого и среднего предпринимательства"
                val insertRec = con.prepareStatement("INSERT INTO ${Prefix}requirement SET id_lot = ?, content = ?")
                insertRec.setInt(1, idLot)
                insertRec.setString(2, recContent)
                insertRec.executeUpdate()
                insertRec.close()
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