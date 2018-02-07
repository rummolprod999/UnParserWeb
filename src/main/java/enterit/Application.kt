package enterit

import enterit.parsers.ParserEtpRf

fun main(args: Array<String>) {
    init(args)
    parserEtpRf()
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

    logger("Добавили тендеров ${AddTenderEtpRf}")
    logger("Конец парсинга")
}