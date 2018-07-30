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
        Arguments.MIRATORG -> parserMiratorg()
        Arguments.STG -> parserStg()
        Arguments.BASHNEFT -> parserBashneft()
        Arguments.RFP -> parserRfp()
        Arguments.ZAKUPKI -> parserZakupki()
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
    logger("Обновили тендеров $UpTenderEtpRf")
    logger("Конец парсинга")
}

fun parserGpn() {
    logger("Начало парсинга")
    val p = ParserGpn()
    p.parser()
    logger("Добавили тендеров $AddTenderGpn")
    logger("Обновили тендеров $UpTenderGpn")
    logger("Конец парсинга")
}

fun parserPol() {
    logger("Начало парсинга")
    val p = ParserPol()
    p.parser()
    logger("Добавили тендеров $AddTenderPol")
    logger("Обновили тендеров $UpTenderPol")
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
    logger("Обновили тендеров $UpTenderLuk")
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
    logger("Обновили тендеров $UpTenderTat")
    logger("Конец парсинга")
}

fun parserRts() {
    logger("Начало парсинга")
    val p = ParserRtsNew()
    p.parser()
    logger("Добавили тендеров $AddTenderRts")
    logger("Обновили тендеров $UpTenderRts")
    logger("Конец парсинга")
}

fun parserSibur() {
    logger("Начало парсинга")
    val p = ParserSibur()
    p.parser()
    logger("Добавили тендеров $AddTenderSibur")
    logger("Обновили тендеров $UpTenderSibur")
    logger("Конец парсинга")
}

fun parserUral() {
    logger("Начало парсинга")
    val p = ParserUral()
    p.parser()
    logger("Добавили тендеров $AddTenderUral")
    logger("Обновили тендеров $UpTenderUral")
    logger("Конец парсинга")
}

fun parserMiratorg() {
    logger("Начало парсинга")
    val p = ParserMiratorg()
    p.parser()
    logger("Добавили тендеров $AddTenderMiratorg")
    logger("Обновили тендеров $UpTenderMiratorg")
    logger("Конец парсинга")
}

fun parserStg() {
    logger("Начало парсинга")
    val p = ParserStg()
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
            logger("Error in parserStg function", e.stackTrace, e)
            e.printStackTrace()
        }
    }
    logger("Добавили тендеров $AddTenderStg")
    logger("Обновили тендеров $UpTenderStg")
    logger("Конец парсинга")
}

fun parserBashneft() {
    logger("Начало парсинга")
    val p = ParserBashneft()
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
            logger("Error in parserBashneft function", e.stackTrace, e)
            e.printStackTrace()
        }
    }
    logger("Добавили тендеров $AddTenderBashneft")
    logger("Обновили тендеров $UpTenderBashneft")
    logger("Конец парсинга")
}

fun parserRfp() {
    logger("Начало парсинга")
    val p = ParserRfp()
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
            logger("Error in parserRfp function", e.stackTrace, e)
            e.printStackTrace()
        }
    }
    logger("Добавили тендеров $AddTenderRfp")
    logger("Обновили тендеров $UpTenderRfp")
    logger("Конец парсинга")
}

fun parserZakupki() {
    logger("Начало парсинга")
    val p = ParserZakupki()
    p.parser()
    logger("Добавили тендеров $AddTenderZakupki")
    logger("Обновили тендеров $UpTenderZakupki")
    logger("Конец парсинга")
}