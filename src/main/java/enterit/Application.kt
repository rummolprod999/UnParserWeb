package enterit

import enterit.parsers.ParserEtpRf
import enterit.parsers.ParserGpn
import enterit.parsers.ParserLuk
import enterit.parsers.ParserPol

fun main(args: Array<String>) {
    init(args)
    when (arg) {

        Arguments.ETPRF -> parserEtpRf()
        Arguments.GPN -> parserGpn()
        Arguments.POL -> parserPol()
        Arguments.LUK -> parserLuk()
    }

}

fun parserEtpRf() {
    logger("Начало парсинга")
    val p = ParserEtpRf()
    var tr = 0
    while (true) {
        try {
            p.parser()
            break
        } catch (e: Exception) {
            tr++
            if (tr > 4) {
                logger("Количество попыток истекло, выходим из программы")
                break
            }
            logger("Error in parserEtpRf function", e.stackTrace, e)
            e.printStackTrace()
        }
    }

    logger("Добавили тендеров $AddTenderEtpRf")
    logger("Конец парсинга")
}

fun parserGpn() {
    logger("Начало парсинга")
    val p = ParserGpn()
    p.parser()
    logger("Добавили тендеров $AddTenderGpn")
    logger("Конец парсинга")
}

fun parserPol() {
    logger("Начало парсинга")
    val p = ParserPol()
    p.parser()
    logger("Добавили тендеров $AddTenderPol")
    logger("Конец парсинга")
}

fun parserLuk() {
    logger("Начало парсинга")
    val p = ParserLuk()
    var tr = 0
    while (true) {
        try {
            p.parser()
            break
        } catch (e: Exception) {
            tr++
            if (tr > 4) {
                logger("Количество попыток истекло, выходим из программы")
                break
            }
            logger("Error in parserLuk function", e.stackTrace, e)
            e.printStackTrace()
        }

    }
    logger("Добавили тендеров $AddTenderLuk")
    logger("Конец парсинга")
}