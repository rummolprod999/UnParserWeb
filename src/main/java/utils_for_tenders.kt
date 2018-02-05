import java.sql.*
import java.util.*
import java.util.Date
import java.util.regex.Matcher
import java.util.regex.Pattern


fun getConformity(conf: String): Int {
    val s = conf.toLowerCase()
    return when {
        s.contains("открыт") -> 5
        s.contains("аукцион") -> 1
        s.contains("котиров") -> 2
        s.contains("предложен") -> 3
        s.contains("единств") -> 4
        else -> 6
    }
}

@Throws(SQLException::class, ClassNotFoundException::class, IllegalAccessException::class, InstantiationException::class)
fun addVNum(con: Connection, id: String, typeFz: Int) {
    var verNum = 1
    val p1: PreparedStatement = con.prepareStatement("SELECT id_tender FROM ${Prefix}tender WHERE purchase_number = ? AND type_fz = ? ORDER BY UNIX_TIMESTAMP(date_version) ASC")
    p1.setString(1, id)
    p1.setInt(2, typeFz)
    val r1: ResultSet = p1.executeQuery()
    while (r1.next()) {
        val IdTender = r1.getInt(1)
        con.prepareStatement("UPDATE ${Prefix}tender SET num_version = ? WHERE id_tender = ? AND type_fz = ?").apply {
            setInt(1, verNum)
            setInt(2, IdTender)
            setInt(3, typeFz)
            executeUpdate()
            close()
        }
        verNum++
    }
    r1.close()
    p1.close()

}

@Throws(SQLException::class, ClassNotFoundException::class, IllegalAccessException::class, InstantiationException::class)
fun tenderKwords(idTender: Int, con: Connection) {
    val s = StringBuilder()
    val p1: PreparedStatement = con.prepareStatement("SELECT DISTINCT po.name, po.okpd_name FROM ${Prefix}purchase_object AS po LEFT JOIN ${Prefix}lot AS l ON l.id_lot = po.id_lot WHERE l.id_tender = ?")
    p1.setInt(1, idTender)
    val r1: ResultSet = p1.executeQuery()
    while (r1.next()) {
        var name: String? = r1.getString(1)
        if (name == null) {
            name = ""
        }
        var okpdName: String? = r1.getString(2)
        if (okpdName == null) {
            okpdName = ""
        }
        with(s) {
            append(" $name")
            append(" $okpdName")
        }
    }
    r1.close()
    p1.close()
    val p2: PreparedStatement = con.prepareStatement("SELECT DISTINCT file_name FROM ${Prefix}attachment WHERE id_tender = ?")
    p2.setInt(1, idTender)
    val r2: ResultSet = p2.executeQuery()
    while (r2.next()) {
        var attName: String? = r2.getString(1)
        if (attName == null) {
            attName = ""
        }
        s.append(" $attName")
    }
    r2.close()
    p2.close()
    var idOrg = 0
    val p3: PreparedStatement = con.prepareStatement("SELECT purchase_object_info, id_organizer FROM ${Prefix}tender WHERE id_tender = ?")
    p3.setInt(1, idTender)
    val r3: ResultSet = p3.executeQuery()
    while (r3.next()) {
        idOrg = r3.getInt(2)
        val purOb = r3.getString(1)
        s.append(" $purOb")
    }
    r3.close()
    p3.close()
    if (idOrg != 0) {
        val p4: PreparedStatement = con.prepareStatement("SELECT full_name, inn FROM ${Prefix}organizer WHERE id_organizer = ?")
        p4.setInt(1, idOrg)
        val r4: ResultSet = p4.executeQuery()
        while (r4.next()) {
            var innOrg: String? = r4.getString(2)
            if (innOrg == null) {
                innOrg = ""
            }
            var nameOrg: String? = r4.getString(1)
            if (nameOrg == null) {
                nameOrg = ""
            }
            with(s) {
                append(" $innOrg")
                append(" $nameOrg")
            }

        }
        r4.close()
        p4.close()
    }
    val p5: PreparedStatement = con.prepareStatement("SELECT DISTINCT cus.inn, cus.full_name FROM ${Prefix}customer AS cus LEFT JOIN ${Prefix}purchase_object AS po ON cus.id_customer = po.id_customer LEFT JOIN ${Prefix}lot AS l ON l.id_lot = po.id_lot WHERE l.id_tender = ?")
    p5.setInt(1, idOrg)
    val r5: ResultSet = p5.executeQuery()
    while (r5.next()) {
        var fullNameC: String?
        fullNameC = r5.getString(1)
        if (fullNameC == null) {
            fullNameC = ""
        }
        var innC: String?
        innC = r5.getString(2)
        if (innC == null) {
            innC = ""
        }
        with(s) {
            append(" $innC")
            append(" $fullNameC")
        }
    }
    r5.close()
    p5.close()
    val pattern: Pattern = Pattern.compile("\\s+")
    val matcher: Matcher = pattern.matcher(s.toString())
    var ss: String = matcher.replaceAll(" ")
    ss = ss.trim { it <= ' ' }
    val p6 = con.prepareStatement("UPDATE ${Prefix}tender SET tender_kwords = ? WHERE id_tender = ?")
    p6.setString(1, ss)
    p6.setInt(2, idTender)
    p6.executeUpdate()
    p6.close()

}

fun getDate(dt: String): Date {
    var d = Date(0L)
    try {
        d = formatter.parseObject(dt) as Date
    } catch (e: Exception) {
        try {
            d = formatterOnlyDate.parseObject(dt) as Date
        } catch (e: Exception) {
        }
    }

    return d
}

fun dateAddHours(dt: Date, h: Int): Date {
    val cal = Calendar.getInstance()
    cal.time = dt
    cal.add(Calendar.HOUR_OF_DAY, h)
    return cal.time
}

fun extractNum(s: String): String {
    var nm = ""
    try {
        val pattern: Pattern = Pattern.compile("\\s+")
        val matcher: Matcher = pattern.matcher(s)
        val s = matcher.replaceAll("")
        val p = Pattern.compile("""(\d+\.*\d*)""")
        val m = p.matcher(s)
        if (m.find()) {
            nm = m.group()
        }
    } catch (e: Exception) {
    }
    return nm
}