package enterit

import org.w3c.dom.Node
import java.io.File
import java.text.Format
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory

val executePath: String = File(Class.forName("enterit.ApplicationKt").protectionDomain.codeSource.location.path).parentFile.toString()
const val arguments = "etprf, gpn, pol, luk, tat, rts, sibur, ural, miratorg, stg, bashneft, mosreg, zakupki, rtsrzd"
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
var tempDirTendersMiratorg: String? = null
var logDirTendersMiratorg: String? = null
var tempDirTendersStg: String? = null
var logDirTendersStg: String? = null
var tempDirTendersBashneft: String? = null
var logDirTendersBashneft: String? = null
var tempDirTenderRfp: String? = null
var logDirTendersRfp: String? = null
var tempDirTenderZakupki: String? = null
var logDirTendersZakupki: String? = null
var tempDirTenderRtsRzd: String? = null
var logDirTendersRtsRzd: String? = null
var UserStg: String? = null
var PassStg: String? = null
var Prefix: String? = null
var UserDb: String? = null
var PassDb: String? = null
var Server: String? = null
var Port: Int = 0
var CountStg: Int = 0
var logPath: String? = null
val DateNow = Date()
var AddTenderEtpRf: Int = 0
var UpTenderEtpRf: Int = 0
var AddTenderGpn: Int = 0
var UpTenderGpn: Int = 0
var AddTenderPol: Int = 0
var UpTenderPol: Int = 0
var AddTenderLuk: Int = 0
var UpTenderLuk: Int = 0
var AddTenderTat: Int = 0
var UpTenderTat: Int = 0
var AddTenderRts: Int = 0
var UpTenderRts: Int = 0
var AddTenderSibur: Int = 0
var UpTenderSibur: Int = 0
var AddTenderUral: Int = 0
var UpTenderUral: Int = 0
var AddTenderMiratorg: Int = 0
var UpTenderMiratorg: Int = 0
var AddTenderStg: Int = 0
var UpTenderStg: Int = 0
var AddTenderBashneft: Int = 0
var UpTenderBashneft: Int = 0
var AddTenderRfp: Int = 0
var UpTenderRfp: Int = 0
var AddTenderZakupki: Int = 0
var UpTenderZakupki: Int = 0
var AddTenderRtsRzd: Int = 0
var UpTenderRtsRzd: Int = 0
var UrlConnect: String? = null
var formatter: Format = SimpleDateFormat("dd.MM.yyyy kk:mm:ss")
var formatterGpn: SimpleDateFormat = SimpleDateFormat("dd.MM.yyyy kk:mm")
var formatterOnlyDate: Format = SimpleDateFormat("dd.MM.yyyy")
var formatterZakupkiDate: Format = SimpleDateFormat("yyyy-MM-dd")
var formatterZakupkiDateTime: Format = SimpleDateFormat("yyyy-MM-dd kk:mm:ss")
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
                    "tempdir_tenders_miratorg" -> tempDirTendersMiratorg = executePath + File.separator + it.childNodes.item(0).textContent
                    "logdir_tenders_miratorg" -> logDirTendersMiratorg = executePath + File.separator + it.childNodes.item(0).textContent
                    "tempdir_tenders_stg" -> tempDirTendersStg = executePath + File.separator + it.childNodes.item(0).textContent
                    "logdir_tenders_stg" -> logDirTendersStg = executePath + File.separator + it.childNodes.item(0).textContent
                    "tempdir_tenders_bashneft" -> tempDirTendersBashneft = executePath + File.separator + it.childNodes.item(0).textContent
                    "logdir_tenders_bashneft" -> logDirTendersBashneft = executePath + File.separator + it.childNodes.item(0).textContent
                    "tempdir_tenders_rfp" -> tempDirTenderRfp = executePath + File.separator + it.childNodes.item(0).textContent
                    "logdir_tenders_rfp" -> logDirTendersRfp = executePath + File.separator + it.childNodes.item(0).textContent
                    "tempdir_tenders_zakupki" -> tempDirTenderZakupki = executePath + File.separator + it.childNodes.item(0).textContent
                    "logdir_tenders_zakupki" -> logDirTendersZakupki = executePath + File.separator + it.childNodes.item(0).textContent
                    "tempdir_tenders_rtsrzd" -> tempDirTenderRtsRzd = executePath + File.separator + it.childNodes.item(0).textContent
                    "logdir_tenders_rtsrzd" -> logDirTendersRtsRzd = executePath + File.separator + it.childNodes.item(0).textContent
                    "prefix" -> Prefix = try {
                        it.childNodes.item(0).textContent
                    } catch (e: Exception) {
                        ""
                    }

                    "userstg" -> UserStg = it.childNodes.item(0).textContent
                    "passstg" -> PassStg = it.childNodes.item(0).textContent
                    "userdb" -> UserDb = it.childNodes.item(0).textContent
                    "passdb" -> PassDb = it.childNodes.item(0).textContent
                    "server" -> Server = it.childNodes.item(0).textContent
                    "port" -> Port = Integer.valueOf(it.childNodes.item(0).textContent)
                    "count_page_stg" -> CountStg = Integer.valueOf(it.childNodes.item(0).textContent)
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
            "miratorg" -> arg = Arguments.MIRATORG
            "stg" -> arg = Arguments.STG
            "bashneft" -> arg = Arguments.BASHNEFT
            "rfp" -> arg = Arguments.RFP
            "zakupki" -> arg = Arguments.ZAKUPKI
            "rtsrzd" -> arg = Arguments.RTSRZD
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
        Arguments.MIRATORG -> run { tempDirTenders = tempDirTendersMiratorg; logDirTenders = logDirTendersMiratorg }
        Arguments.STG -> run { tempDirTenders = tempDirTendersStg; logDirTenders = logDirTendersStg }
        Arguments.BASHNEFT -> run { tempDirTenders = tempDirTendersBashneft; logDirTenders = logDirTendersBashneft }
        Arguments.RFP -> run { tempDirTenders = tempDirTenderRfp; logDirTenders = logDirTendersRfp }
        Arguments.ZAKUPKI -> run { tempDirTenders = tempDirTenderZakupki; logDirTenders = logDirTendersZakupki }
        Arguments.RTSRZD -> run { tempDirTenders = tempDirTenderRtsRzd; logDirTenders = logDirTendersRtsRzd }
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