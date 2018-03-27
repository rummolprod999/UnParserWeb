package enterit

import org.w3c.dom.Node
import java.io.File
import java.text.Format
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory

val executePath: String = File(Class.forName("enterit.ApplicationKt").protectionDomain.codeSource.location.path).parentFile.toString()
const val arguments = "etprf, gpn, pol, luk, tat, rts, sibur, ural"
lateinit var arg: Arguments
var Database: String? = null
var tempDirTenders: String? = null
var logDirTenders: String? = null
var tempDirTendersEtpRf: String? = null
var logDirTendersEtpRf: String? = null
var tempDirTendersGpn: String? = null
var logDirTendersGpn: String? = null
var tempDirTendersPol: String? = null
var logDirTendersPol: String? = null
var tempDirTendersLuk: String? = null
var logDirTendersLuk: String? = null
var tempDirTendersTat: String? = null
var logDirTendersTat: String? = null
var tempDirTendersRts: String? = null
var logDirTendersRts: String? = null
var tempDirTendersSibur: String? = null
var logDirTendersSibur: String? = null
var tempDirTendersUral: String? = null
var logDirTendersUral: String? = null
var Prefix: String? = null
var UserDb: String? = null
var PassDb: String? = null
var Server: String? = null
var Port: Int = 0
var logPath: String? = null
val DateNow = Date()
var AddTenderEtpRf: Int = 0
var AddTenderGpn: Int = 0
var AddTenderPol: Int = 0
var AddTenderLuk: Int = 0
var AddTenderTat: Int = 0
var AddTenderRts: Int = 0
var AddTenderSibur: Int = 0
var AddTenderUral: Int = 0
var UrlConnect: String? = null
var formatter: Format = SimpleDateFormat("dd.MM.yyyy kk:mm:ss")
var formatterGpn: SimpleDateFormat = SimpleDateFormat("dd.MM.yyyy kk:mm")
var formatterOnlyDate: Format = SimpleDateFormat("dd.MM.yyyy")
var formatterEtpRf: Format = SimpleDateFormat("dd.MM.yyyy kk:mm:ss (XXX)")
var formatterEtpRfN: Format = SimpleDateFormat("dd.MM.yyyy kk:mm (XXX)")

fun getSettings() = try {
    val filePathSetting = executePath + File.separator + "setting_tenders.xml"
    val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    val document = documentBuilder.parse(filePathSetting)
    val root = document.documentElement
    val settings = root.childNodes
    (0 until settings.length)
            .asSequence()
            .map { settings.item(it) }
            .filter {
                @Suppress("DEPRECATED_IDENTITY_EQUALS")
                it.nodeType !== Node.TEXT_NODE
            }
            .forEach {
                when (it.nodeName) {
                    "database" -> Database = it.childNodes.item(0).textContent
                    "tempdir_tenders_etprf" -> tempDirTendersEtpRf = executePath + File.separator + it.childNodes.item(0).textContent
                    "logdir_tenders_etprf" -> logDirTendersEtpRf = executePath + File.separator + it.childNodes.item(0).textContent
                    "tempdir_tenders_gpn" -> tempDirTendersGpn = executePath + File.separator + it.childNodes.item(0).textContent
                    "logdir_tenders_gpn" -> logDirTendersGpn = executePath + File.separator + it.childNodes.item(0).textContent
                    "tempdir_tenders_pol" -> tempDirTendersPol = executePath + File.separator + it.childNodes.item(0).textContent
                    "logdir_tenders_pol" -> logDirTendersPol = executePath + File.separator + it.childNodes.item(0).textContent
                    "tempdir_tenders_luk" -> tempDirTendersLuk = executePath + File.separator + it.childNodes.item(0).textContent
                    "logdir_tenders_luk" -> logDirTendersLuk = executePath + File.separator + it.childNodes.item(0).textContent
                    "tempdir_tenders_tat" -> tempDirTendersTat = executePath + File.separator + it.childNodes.item(0).textContent
                    "logdir_tenders_tat" -> logDirTendersTat = executePath + File.separator + it.childNodes.item(0).textContent
                    "tempdir_tenders_rts" -> tempDirTendersRts = executePath + File.separator + it.childNodes.item(0).textContent
                    "logdir_tenders_rts" -> logDirTendersRts = executePath + File.separator + it.childNodes.item(0).textContent
                    "tempdir_tenders_sibur" -> tempDirTendersSibur = executePath + File.separator + it.childNodes.item(0).textContent
                    "logdir_tenders_sibur" -> logDirTendersSibur = executePath + File.separator + it.childNodes.item(0).textContent
                    "tempdir_tenders_ural" -> tempDirTendersUral = executePath + File.separator + it.childNodes.item(0).textContent
                    "logdir_tenders_ural" -> logDirTendersUral = executePath + File.separator + it.childNodes.item(0).textContent
                    "prefix" -> Prefix = try {
                        it.childNodes.item(0).textContent
                    } catch (e: Exception) {
                        ""
                    }

                    "userdb" -> UserDb = it.childNodes.item(0).textContent
                    "passdb" -> PassDb = it.childNodes.item(0).textContent
                    "server" -> Server = it.childNodes.item(0).textContent
                    "port" -> Port = Integer.valueOf(it.childNodes.item(0).textContent)
                }
            }
} catch (e: Exception) {
    e.printStackTrace()
    System.exit(1)
}

fun init(args: Array<String>) {
    if (args.isEmpty()) {
        println("Недостаточно агрументов для запуска, используйте $arguments для запуска")
        System.exit(0)
    } else {
        when (args[0]) {
            "etprf" -> arg = Arguments.ETPRF
            "gpn" -> arg = Arguments.GPN
            "pol" -> arg = Arguments.POL
            "luk" -> arg = Arguments.LUK
            "tat" -> arg = Arguments.TAT
            "rts" -> arg = Arguments.RTS
            "sibur" -> arg = Arguments.SIBUR
            "ural" -> arg = Arguments.URAL
            else -> run { println("Неверно указаны аргументы, используйте $arguments, выходим из программы"); System.exit(0) }

        }
    }
    getSettings()
    when (arg) {
        Arguments.ETPRF -> run { tempDirTenders = tempDirTendersEtpRf; logDirTenders = logDirTendersEtpRf }
        Arguments.GPN -> run { tempDirTenders = tempDirTendersGpn; logDirTenders = logDirTendersGpn }
        Arguments.POL -> run { tempDirTenders = tempDirTendersPol; logDirTenders = logDirTendersPol }
        Arguments.LUK -> run { tempDirTenders = tempDirTendersLuk; logDirTenders = logDirTendersLuk }
        Arguments.TAT -> run { tempDirTenders = tempDirTendersTat; logDirTenders = logDirTendersTat }
        Arguments.RTS -> run { tempDirTenders = tempDirTendersRts; logDirTenders = logDirTendersRts }
        Arguments.SIBUR -> run { tempDirTenders = tempDirTendersSibur; logDirTenders = logDirTendersSibur }
        Arguments.URAL -> run { tempDirTenders = tempDirTendersUral; logDirTenders = logDirTendersUral }
    }
    if (tempDirTenders == null || tempDirTenders == "") {
        println("Не задана папка для временных файлов, выходим из программы")
        System.exit(0)
    }
    if (logDirTenders == null || logDirTenders == "") {
        println("Не задана папка для логов, выходим из программы")
        System.exit(0)
    }
    val tmp = File(tempDirTenders)
    if (tmp.exists()) {
        tmp.delete()
        tmp.mkdir()
    } else {
        tmp.mkdir()
    }
    val log = File(logDirTenders)
    if (!log.exists()) {
        log.mkdir()
    }
    val dateFormat = SimpleDateFormat("yyyy-MM-dd")
    logPath = "$logDirTenders${File.separator}log_parsing_${arg}_${dateFormat.format(DateNow)}.log"
    UrlConnect = "jdbc:mysql://$Server:$Port/$Database?jdbcCompliantTruncation=false&useUnicode=true&characterEncoding=utf-8&useLegacyDatetimeCode=false&serverTimezone=Europe/Moscow&connectTimeout=5000&socketTimeout=30000"
}