package enterit

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Node
import java.text.Format
import java.text.SimpleDateFormat
import java.util.*

val executePath: String = File(Class.forName("enterit.ApplicationKt").protectionDomain.codeSource.location.path).parentFile.toString()
const val arguments = "etprf"
lateinit var arg: Arguments
var Database: String? = null
var tempDirTenders: String? = null
var logDirTenders: String? = null
var tempDirTendersEtpRf: String? = null
var logDirTendersEtpRf: String? = null
var Prefix: String? = null
var UserDb: String? = null
var PassDb: String? = null
var Server: String? = null
var Port: Int = 0
var logPath: String? = null
val DateNow = Date()
var AddTenderEtpRf: Int = 0
var UrlConnect: String? = null
var formatter: Format = SimpleDateFormat("dd.MM.yyyy kk:mm:ss")
var formatterOnlyDate: Format = SimpleDateFormat("dd.MM.yyyy")
var formatterEtpRf: Format = SimpleDateFormat("dd.MM.yyyy kk:mm:ss (XXX)")

fun GetSettings() = try {
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
            else -> run { println("Неверно указаны аргументы, используйте $arguments, выходим из программы"); System.exit(0) }

        }
    }
    GetSettings()
    when (arg) {
        Arguments.ETPRF -> run { tempDirTenders = tempDirTendersEtpRf; logDirTenders = logDirTendersEtpRf }
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
    logPath = "${logDirTenders}${File.separator}log_parsing_etprf_${dateFormat.format(DateNow)}.log"
    UrlConnect = "jdbc:mysql://${Server}:${Port}/${Database}?jdbcCompliantTruncation=false&useUnicode=true&characterEncoding=utf-8&useLegacyDatetimeCode=false&serverTimezone=Europe/Moscow&connectTimeout=5000&socketTimeout=30000"
}