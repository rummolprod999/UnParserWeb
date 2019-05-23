package enterit

import enterit.tenders.TenderInfoPol
import enterit.tenders.TenderPol
import enterit.tenders.TenderPolNew
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.text.Format
import java.text.SimpleDateFormat
import java.util.*
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
fun tenderKwords(idTender: Int, con: Connection, addInfo: String = "") {
    val s = StringBuilder()
    if (addInfo != "") with(s) { append(addInfo) }
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

fun getDateFromFormat(dt: String, format: Format): Date {
    var d = Date(0L)
    try {
        d = format.parseObject(dt) as Date
    } catch (e: Exception) {
    }

    return d
}

fun String.getDateFromString(format: Format): Date {
    var d = Date(0L)
    if (this == "") return d
    try {
        d = format.parseObject(this) as Date
    } catch (e: Exception) {
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
        val ss = matcher.replaceAll("")
        val p = Pattern.compile("""(\d+\.*\d*)""")
        val m = p.matcher(ss)
        if (m.find()) {
            nm = m.group()
        }
    } catch (e: Exception) {
    }
    return nm
}

fun returnPriceEtpRf(s: String): String {
    var t = ""
    val tt = s.replace(',', '.')
    val pattern: Pattern = Pattern.compile("\\s+")
    val matcher: Matcher = pattern.matcher(tt)
    t = matcher.replaceAll("")
    return t
}

fun String.extractPrice(): String {
    var nm = ""
    try {
        val pattern: Pattern = Pattern.compile("\\s+")
        val tt = this.replace(',', '.')
        val matcher: Matcher = pattern.matcher(tt)
        val ss = matcher.replaceAll("")
        val p = Pattern.compile("""(\d+\.*\d*)""")
        val m = p.matcher(ss)
        if (m.find()) {
            nm = m.group(1)
        }
    } catch (e: Exception) {
    }
    return nm
}

fun getOkpd(s: String): Pair<Int, String> {
    var okpd2GroupCode = 0
    var okpd2GroupLevel1Code = ""
    if (s.length > 1) {
        val dot = s.indexOf('.')
        if (dot != -1) {
            val okpd2GroupCodeTemp = s.slice(0 until dot)
            try {
                okpd2GroupCode = Integer.parseInt(okpd2GroupCodeTemp)
            } catch (e: Exception) {
            }
        }
    }
    if (s.length > 3) {
        val dot = s.indexOf('.')
        if (dot != -1) {
            okpd2GroupLevel1Code = s.slice(dot + 1 until dot + 2)
        }

    }
    return Pair(okpd2GroupCode, okpd2GroupLevel1Code)
}

fun getDateFromString(d: String): Date {
    val t = Date(0L)
    return t
}

fun String.tryParseInt(): Boolean {
    return try {
        Integer.parseInt(this)
        true
    } catch (e: NumberFormatException) {
        false
    }

}

fun getOffset(s: String): String {
    var g = "GMT+3"
    try {
        val pattern: Pattern = Pattern.compile("""\((\S*)\)""")
        val matcher: Matcher = pattern.matcher(s)
        if (matcher.find()) {
            val mt = matcher.group(1)
            g = mt.replace("UTC", "GMT")
        }
    } catch (e: Exception) {
    }
    return g
}

fun getDateFromFormatOffset(dt: String, format: SimpleDateFormat, offset: String): Date {
    var d = Date(0L)
    try {
        format.timeZone = TimeZone.getTimeZone(offset)
        d = format.parseObject(dt) as Date
    } catch (e: Exception) {
    }

    return d
}

fun TenderPol.GetDates(s: String): TenderInfoPol {
    var startD = Date(0L)
    var endD = Date(0L)
    var status = ""
    val tmpSS = regExpTester("""^Дата публикации:\s(\d+\.\d+\.\d+)""", s)
    startD = getDateFromFormat(tmpSS, formatterOnlyDate)
    val tmpSE = regExpTester("""Прием заявок до:\s(\d+\.\d+\.\d+)""", s)
    endD = getDateFromFormat(tmpSE, formatterOnlyDate)
    val tmpST = regExpTester("""Статус закупки:\s(\w+|\W+)${'$'}""", s)
    status = tmpST
    return TenderInfoPol(startD, endD, status)
}

fun TenderPolNew.GetDates(s: String): TenderInfoPol {
    var startD = Date(0L)
    var endD = Date(0L)
    var status = ""
    val tmpSS = regExpTester("""^Дата публикации:\s(\d+\.\d+\.\d+)""", s)
    startD = getDateFromFormat(tmpSS, formatterOnlyDate)
    val tmpSE = regExpTester("""Прием заявок до:\s(\d+\.\d+\.\d+)""", s)
    endD = getDateFromFormat(tmpSE, formatterOnlyDate)
    val tmpST = regExpTester("""Статус закупки:\s(\w+|\W+)${'$'}""", s)
    status = tmpST
    return TenderInfoPol(startD, endD, status)
}

fun regExpTester(reg: String, s: String): String {
    var st = ""
    try {
        val pattern: Pattern = Pattern.compile(reg)
        val matcher: Matcher = pattern.matcher(s)
        if (matcher.find()) {
            st = matcher.group(1)
        }
    } catch (e: Exception) {
    }
    return st.trim { it <= ' ' }
}

fun String.regExpTest(reg: String): String {
    var st = ""
    try {
        val pattern: Pattern = Pattern.compile(reg)
        val matcher: Matcher = pattern.matcher(this)
        if (matcher.find()) {
            st = matcher.group(1)
        }
    } catch (e: Exception) {
    }
    return st.trim { it <= ' ' }
}

fun getRegion(sp: String): String {
    val s = sp.toLowerCase()
    return when {
        s.contains("белгор") -> "белгор"
        s.contains("брянск") -> "брянск"
        s.contains("владимир") -> "владимир"
        s.contains("воронеж") -> "воронеж"
        s.contains("иванов") -> "иванов"
        s.contains("калужск") -> "калужск"
        s.contains("костром") -> "костром"
        s.contains("курск") -> "курск"
        s.contains("липецк") -> "липецк"
        s.contains("москва") -> "москва"
        s.contains("московск") -> "московск"
        s.contains("орлов") -> "орлов"
        s.contains("рязан") -> "рязан"
        s.contains("смолен") -> "смолен"
        s.contains("тамбов") -> "тамбов"
        s.contains("твер") -> "твер"
        s.contains("тульс") -> "тульс"
        s.contains("яросл") -> "яросл"
        s.contains("архан") -> "архан"
        s.contains("вологод") -> "вологод"
        s.contains("калинин") -> "калинин"
        s.contains("карел") -> "карел"
        s.contains("коми") -> "коми"
        s.contains("ленинг") -> "ленинг"
        s.contains("мурм") -> "мурм"
        s.contains("ненец") -> "ненец"
        s.contains("новгор") -> "новгор"
        s.contains("псков") -> "псков"
        s.contains("санкт") -> "санкт"
        s.contains("адыг") -> "адыг"
        s.contains("астрахан") -> "астрахан"
        s.contains("волгог") -> "волгог"
        s.contains("калмык") -> "калмык"
        s.contains("краснод") -> "краснод"
        s.contains("ростов") -> "ростов"
        s.contains("дагест") -> "дагест"
        s.contains("ингуш") -> "ингуш"
        s.contains("кабардин") -> "кабардин"
        s.contains("карача") -> "карача"
        s.contains("осети") -> "осети"
        s.contains("ставроп") -> "ставроп"
        s.contains("чечен") -> "чечен"
        s.contains("башкор") -> "башкор"
        s.contains("киров") -> "киров"
        s.contains("марий") -> "марий"
        s.contains("мордов") -> "мордов"
        s.contains("нижегор") -> "нижегор"
        s.contains("оренбур") -> "оренбур"
        s.contains("пензен") -> "пензен"
        s.contains("пермс") -> "пермс"
        s.contains("самар") -> "самар"
        s.contains("сарат") -> "сарат"
        s.contains("татарс") -> "татарс"
        s.contains("удмурт") -> "удмурт"
        s.contains("ульян") -> "ульян"
        s.contains("чуваш") -> "чуваш"
        s.contains("курган") -> "курган"
        s.contains("свердлов") -> "свердлов"
        s.contains("тюмен") -> "тюмен"
        s.contains("ханты") -> "ханты"
        s.contains("челяб") -> "челяб"
        s.contains("ямало") -> "ямало"
        s.contains("алтайск") -> "алтайск"
        s.contains("алтай") -> "алтай"
        s.contains("бурят") -> "бурят"
        s.contains("забайк") -> "забайк"
        s.contains("иркут") -> "иркут"
        s.contains("кемеров") -> "кемеров"
        s.contains("краснояр") -> "краснояр"
        s.contains("новосиб") -> "новосиб"
        s.contains("томск") -> "томск"
        s.contains("омск") -> "омск"
        s.contains("тыва") -> "тыва"
        s.contains("хакас") -> "хакас"
        s.contains("амурск") -> "амурск"
        s.contains("еврей") -> "еврей"
        s.contains("камчат") -> "камчат"
        s.contains("магад") -> "магад"
        s.contains("примор") -> "примор"
        s.contains("сахалин") -> "сахалин"
        s.contains("якут") -> "якут"
        s.contains("саха") -> "саха"
        s.contains("хабар") -> "хабар"
        s.contains("чукот") -> "чукот"
        s.contains("крым") -> "крым"
        s.contains("севастоп") -> "севастоп"
        s.contains("байкон") -> "байкон"
        else -> ""
    }

}