package enterit.tenders

import enterit.*
import org.jsoup.Jsoup
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.sql.Timestamp
import java.util.*

data class TenderRfp(val status: String, val datePub: Date, val dateEnd: Date, val url: String) : TenderAbstract(),
    ITender {
    companion object TypeFz {
        val typeFz = 54
    }

    init {
        etpName = "Электронная площадка RFP"
        etpUrl = "https://www.rfp.ltd"
    }

    override fun parsing() {
        val stPage = downloadFromUrl(url)
        if (stPage == "") {
            logger("Gets empty string TenderRfp", url)
            return
        }
        val html = Jsoup.parse(stPage)
        var purNum =
            html.selectFirst("div.podlojka div.row div:contains(№ лота:) + p > span")?.ownText()?.trim { it <= ' ' }
                ?: ""
        if (purNum == "") {
            logger("Empty purchase number in $url")
            return
        }
        purNum = purNum.replace("""\s+""".toRegex(), "")
        DriverManager.getConnection(UrlConnect, UserDb, PassDb).use(fun(con: Connection) {
            val stmt0 =
                con.prepareStatement("SELECT id_tender FROM ${Prefix}tender WHERE purchase_number = ? AND doc_publish_date = ? AND type_fz = ? AND notice_version = ? AND end_date = ?")
                    .apply {
                        setString(1, purNum)
                        setTimestamp(2, Timestamp(datePub.time))
                        setInt(3, typeFz)
                        setString(4, status)
                        setTimestamp(5, Timestamp(dateEnd.time))
                    }
            val r = stmt0.executeQuery()
            if (r.next()) {
                r.close()
                stmt0.close()
                return
            }
            r.close()
            stmt0.close()
            val dateVer = Date()
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
                if (dateVer.after(dateB) || dateB == Timestamp(dateVer.time)) {
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
            val idEtp = getEtp(con)
            var idPlacingWay = 0
            val placingWayName =
                html.selectFirst("div.podlojka div.row div:contains(Вид запроса:) + p")?.text()?.trim { it <= ' ' }
                    ?: ""
            if (placingWayName != "") {
                idPlacingWay = getPlacingWay(con, placingWayName)
            }
            var idRegion = 0
            val regionName =
                html.selectFirst("div.podlojka div.row div:contains(Субъект:) + p")?.text()?.trim { it <= ' ' }
                    ?: ""
            if (regionName != "") {
                idRegion = getIdRegion(con, regionName)
            }
            var idTender = 0
            val purObj = html.selectFirst("h1")?.ownText()?.trim { it <= ' ' }
                ?: ""
            val insertTender = con.prepareStatement(
                "INSERT INTO ${Prefix}tender SET id_xml = ?, purchase_number = ?, doc_publish_date = ?, href = ?, purchase_object_info = ?, type_fz = ?, id_organizer = ?, id_placing_way = ?, id_etp = ?, end_date = ?, cancel = ?, date_version = ?, num_version = ?, notice_version = ?, xml = ?, print_form = ?, id_region = ?, scoring_date = ?",
                Statement.RETURN_GENERATED_KEYS
            )
            insertTender.setString(1, purNum)
            insertTender.setString(2, purNum)
            insertTender.setTimestamp(3, Timestamp(datePub.time))
            insertTender.setString(4, url)
            insertTender.setString(5, purObj)
            insertTender.setInt(6, typeFz)
            insertTender.setInt(7, IdOrganizer)
            insertTender.setInt(8, idPlacingWay)
            insertTender.setInt(9, idEtp)
            insertTender.setTimestamp(10, Timestamp(dateEnd.time))
            insertTender.setInt(11, cancelstatus)
            insertTender.setTimestamp(12, Timestamp(dateVer.time))
            insertTender.setInt(13, 1)
            insertTender.setString(14, status)
            insertTender.setString(15, url)
            insertTender.setString(16, url)
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
                UpTenderRfp++
            } else {
                AddTenderRfp++
            }
            var idLot = 0
            val LotNumber = 1
            val insertLot = con.prepareStatement(
                "INSERT INTO ${Prefix}lot SET id_tender = ?, lot_number = ?",
                Statement.RETURN_GENERATED_KEYS
            )
            insertLot.setInt(1, idTender)
            insertLot.setInt(2, LotNumber)
            insertLot.executeUpdate()
            val rl = insertLot.generatedKeys
            if (rl.next()) {
                idLot = rl.getInt(1)
            }
            rl.close()
            insertLot.close()
            val lotObj = html.selectFirst("div.podlojka div.row div:contains(Отрасль:) + p")?.text()?.trim { it <= ' ' }
                ?: ""
            val purName = "$purObj $lotObj"
            val insertPurObj =
                con.prepareStatement("INSERT INTO ${Prefix}purchase_object SET id_lot = ?, id_customer = ?, name = ?")
            insertPurObj.setInt(1, idLot)
            insertPurObj.setInt(2, 0)
            insertPurObj.setString(3, purName)
            insertPurObj.executeUpdate()
            insertPurObj.close()
            val delivPlace =
                html.selectFirst("div.podlojka div.row div:contains(Место поставки:) + p")?.text()?.trim { it <= ' ' }
                    ?: ""
            if (delivPlace != "") {
                val insertCusRec =
                    con.prepareStatement("INSERT INTO ${Prefix}customer_requirement SET id_lot = ?, id_customer = ?, delivery_term = ?, delivery_place = ?")
                insertCusRec.setInt(1, idLot)
                insertCusRec.setInt(2, 0)
                insertCusRec.setString(3, "")
                insertCusRec.setString(4, delivPlace)
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