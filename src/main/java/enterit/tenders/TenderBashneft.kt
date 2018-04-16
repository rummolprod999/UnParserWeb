package enterit.tenders

import com.gargoylesoftware.htmlunit.html.*
import enterit.*
import enterit.parsers.ParserBashneft
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.sql.Timestamp
import java.util.*

class TenderBashneft(val status: String, val purNum: String, var urlT: String, val purObj: String, var placingWayName: String, private val pubDate: Date, private val endDate: Date) : TenderAbstract(), ITender {
    companion object TypeFz {
        val typeFz = 31
        const val timeoutB = 5000L
    }

    init {
        etpName = "ЭТП «Башнефть»"
        etpUrl = "http://etp.bashneft.ru/"
    }

    override fun parsing() {
        ParserBashneft.webClient.close()
        val dateVer = Date()
        DriverManager.getConnection(UrlConnect, UserDb, PassDb).use(fun(con: Connection) {
            val stmt0 = con.prepareStatement("SELECT id_tender FROM ${Prefix}tender WHERE purchase_number = ? AND doc_publish_date = ? AND type_fz = ? AND notice_version = ? AND end_date = ?")
            stmt0.setString(1, purNum)
            stmt0.setTimestamp(2, Timestamp(pubDate.time))
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
            val page: HtmlPage = ParserBashneft.webClient.getPage(urlT)
            val orgFullName = page.getFirstByXPath<HtmlInput>("//input[@id = 'ctl00_RootContentPlaceHolder_AuctionFormLayout_EnterpriseComboBox_I']")?.getAttribute("value")?.trim { it <= ' ' }
                    ?: ""
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
                    val email = ""
                    val phone = ""
                    val contactPerson = page.getFirstByXPath<HtmlSpan>("//td[contains(preceding-sibling::td, 'Ответственное лицо:')]/span")?.textContent?.trim { it <= ' ' }
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
            var idTender = 0
            /*val stPage = downloadFromUrl(urlT)
            if (stPage == "") {
                logger("Gets empty string TenderBashneft", urlT)
                return
            }
            val html = Jsoup.parse(stPage)
            val addinfo1 = html.selectFirst("#ctl00_RootContentPlaceHolder_AuctionFormLayout_AuctionPageControl_C2")?.text()?.trim { it <= ' ' }*/
            val addInfo = page.getFirstByXPath<HtmlDivision>("//div[@id = 'ctl00_RootContentPlaceHolder_AuctionFormLayout_AuctionPageControl_C2']")?.asText()?.trim { it <= ' ' }
                    ?: ""
            val idEtp = getEtp(con)
            var idPlacingWay = 0
            if (placingWayName != "") {
                idPlacingWay = getPlacingWay(con, placingWayName)
            }
            var idRegion = 0
            val insertTender = con.prepareStatement("INSERT INTO ${Prefix}tender SET id_xml = ?, purchase_number = ?, doc_publish_date = ?, href = ?, purchase_object_info = ?, type_fz = ?, id_organizer = ?, id_placing_way = ?, id_etp = ?, end_date = ?, cancel = ?, date_version = ?, num_version = ?, notice_version = ?, xml = ?, print_form = ?, id_region = ?", Statement.RETURN_GENERATED_KEYS)
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
            insertTender.executeUpdate()
            val rt = insertTender.generatedKeys
            if (rt.next()) {
                idTender = rt.getInt(1)
            }
            rt.close()
            insertTender.close()
            AddTenderBashneft++
            /*val documents = page.getByXPath<HtmlTableRow>("//tr[contains(@id, 'ctl00_RootContentPlaceHolder_AuctionFormLayout_AuctionPageControl_DocumentsGridView_DXDataRow')]")
            documents.forEach {
                val hrefT = it.getCell(3).getElementsByTagName("img")[0]
                val pg = hrefT.click<Page>()
                println(pg.url)
            }*/
            val lots = page.getByXPath<HtmlTableRow>("//tr[contains(@id, 'ctl00_RootContentPlaceHolder_AuctionFormLayout_AuctionPageControl_LotsGridView_DXDataRow')]")
            lots.forEach {
                if (it is HtmlTableRow) {
                    try {
                        parsingLots(it, con, idTender, page, orgFullName)
                    } catch (e: Exception) {
                        logger("error in ${this::class.simpleName}.parsingLots()", e.stackTrace, e, urlT)
                    }
                }
            }
            try {
                tenderKwords(idTender, con, addInfo)
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

    private fun parsingLots(it: HtmlTableRow, con: Connection, idTender: Int, page: HtmlPage, fullnameOrg: String) {
        var idLot = 0
        var lotNumT = it.getCell(1)?.textContent?.trim { it <= ' ' } ?: ""
        lotNumT = lotNumT.regExpTest("""(\d+)${'$'}""")
        val lotNum = Integer.valueOf(lotNumT) ?: 1
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
        val purName = it.getCell(3)?.textContent?.trim { it <= ' ' } ?: ""
        var quantity = it.getCell(4)?.textContent?.trim { it <= ' ' } ?: ""
        var price = it.getCell(5)?.textContent?.trim { it <= ' ' } ?: ""
        val insertPurObj = con.prepareStatement("INSERT INTO ${Prefix}purchase_object SET id_lot = ?, id_customer = ?, name = ?, quantity_value = ?, customer_quantity_value = ?, price = ?")
        insertPurObj.setInt(1, idLot)
        insertPurObj.setInt(2, idCustomer)
        insertPurObj.setString(3, purName)
        insertPurObj.setString(4, quantity)
        insertPurObj.setString(5, quantity)
        insertPurObj.setString(6, price)
        insertPurObj.executeUpdate()
        insertPurObj.close()
    }
}