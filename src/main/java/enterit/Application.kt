package enterit

import enterit.parsers.*

fun main(args: Array<String>) {
    init(args)
    when (arg) {

        Arguments.ETPRF -> parserEtpRf()
        Arguments.GPN -> parserGpn()
        Arguments.POL -> parserPol()
        Arguments.LUK -> parserLuk()
        Arguments.TAT -> parserTat()
        Arguments.RTS -> parserRts()
        Arguments.SIBUR -> parserSibur()
        Arguments.URAL -> parserUral()
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

fun parserTat() {
    logger("Начало парсинга")
    val p = ParserTat()
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
            logger("Error in ParserTat function", e.stackTrace, e)
            e.printStackTrace()
        }

    }
    logger("Добавили тендеров $AddTenderTat")
    logger("Конец парсинга")
}

fun parserRts() {
    logger("Начало парсинга")
    val p = ParserRts()
    p.parser()
    logger("Добавили тендеров $AddTenderRts")
    logger("Конец парсинга")
}

fun parserSibur() {
    logger("Начало парсинга")
    val p = ParserSibur()
    p.parser()
    logger("Добавили тендеров $AddTenderSibur")
    logger("Конец парсинга")
}

fun parserUral() {
    logger("Начало парсинга")
    val p = ParserUral()
    p.parser()
    logger("Добавили тендеров $AddTenderUral")
    logger("Конец парсинга")
}