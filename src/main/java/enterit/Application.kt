package enterit

import enterit.parsers.ParserEtpRf
import enterit.parsers.ParserGpn

fun main(args: Array<String>) {
    init(args)
    when (arg) {

        Arguments.ETPRF -> parserEtpRf()
        Arguments.GPN -> parserGpn()
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
            logger("Error in ParserZakupMos function", e.stackTrace, e)
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