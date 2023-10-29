package enterit.parsers

import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.*
import com.twocaptcha.TwoCaptcha
import com.twocaptcha.captcha.Normal
import enterit.*
import enterit.tenders.TenderEtpRf
import java.io.File
import java.lang.Thread.sleep
import java.net.URL
import java.util.*
import java.util.logging.Level
import java.util.regex.Matcher
import java.util.regex.Pattern


const val PageNumEtpRf = 200

class ParserEtpRf : Iparser {
    val BaseUrl = "https://webppo.etprf.ru"
    val urlEtprf =
        listOf("https://webppo.etprf.ru/NotificationCR", "https://webppo.etprf.ru/NotificationEX", "https://webppo.etprf.ru/BRNotification")

    init {
        java.util.logging.Logger.getLogger("com.gargoylesoftware").level = Level.OFF
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog")
    }

    override fun parser() {
        val webClient = WebClient(BrowserVersion.CHROME)
        webClient.options.isUseInsecureSSL = true
        webClient.options.isThrowExceptionOnScriptError = false
        val page: HtmlPage = webClient.getPage("https://webppo.etprf.ru/Logon/Participants")
        val inputName = page.getHtmlElementById("userName") as HtmlInput;
        inputName.valueAttribute = UserEtprf
        val inputPass = page.getHtmlElementById("password") as HtmlInput;
        inputPass.valueAttribute = PassEtprf
        val captchaSrc = page.getHtmlElementById("ImageKey") as HtmlImage;
        captchaSrc.saveAs(File("./captcha.jpg"))
        val solver = TwoCaptcha(Token)
        solver.setHost("2captcha.com")
        solver.setDefaultTimeout(120);
        solver.setRecaptchaTimeout(600);
        solver.setPollingInterval(10);
        val captcha = Normal()
        captcha.setFile("./captcha.jpg")
        captcha.setMinLen(2)
        captcha.setMaxLen(20)
        captcha.setCaseSensitive(true)
        captcha.setLang("en")
        solver.solve(captcha)
        val inputC = page.getHtmlElementById("TextImageKey") as HtmlInput;
        inputC.valueAttribute = captcha.code
        val inp = page.getFirstByXPath<HtmlButton>("//button[@id='bt_Ok']")
        inp.click<HtmlPage>()
        urlEtprf.forEach { i -> parserE(webClient, i) }
        webClient.close()
    }

    private fun parserE(webClient: WebClient, pg: String) {
        val page: HtmlPage = webClient.getPage(pg)
        val cookies = webClient.getCookies(URL("https://webppo.etprf.ru"))
        Cookies = cookies.map { c -> c.name + "=" + c.value }.joinToString(separator=";")
        if (pg == "https://webppo.etprf.ru/BRNotification" || pg == "https://webppo.etprf.ru/NotificationCR") {
            try {
                parserPage(page)
            } catch (e: Exception) {
                logger("Error in parserE function", e.stackTrace, e)
            }
        } else if (pg == "https://webppo.etprf.ru/NotificationEX") {
            try {
                parserPageN(page)
            } catch (e: Exception) {
                logger("Error in parserE function", e.stackTrace, e)
            }
        }
        for (i in 1..PageNumEtpRf) {
            val button = page.getByXPath<HtmlAnchor>("//a[span[@class = 'ui-button-text' and . = 'Вперед']]")
            val b = button[0] as HtmlAnchor
            val y: HtmlPage = b.click()
            sleep(2000)
            if (pg == "https://webppo.etprf.ru/BRNotification" || pg == "https://webppo.etprf.ru/NotificationCR") {
                try {
                    parserPage(y)
                } catch (e: Exception) {
                    logger("Error in parserPage function", e.stackTrace, e)
                }
            } else if (pg == "https://webppo.etprf.ru/NotificationEX") {
                try {
                    parserPageN(y)
                } catch (e: Exception) {
                    logger("Error in parserPageN function", e.stackTrace, e)
                }
            }

        }
    }

    private fun parserPage(p: HtmlPage) {
        val tends: MutableList<HtmlTableRow> =
            p.getByXPath<HtmlTableRow>("//table[@class = 'reporttable']/tbody/tr[@id]")
        for (i in tends) parserTender(i)
    }

    private fun parserPageN(p: HtmlPage) {
        val tends: MutableList<HtmlTableRow> =
            p.getByXPath<HtmlTableRow>("//table[@class = 'reporttable']/tbody/tr[@id]")
        for (i in tends) parserTenderN(i)
    }

    private fun parserTender(t: HtmlTableRow) {
        try {
            val status = t.getCell(10).textContent.trim { it <= ' ' }
            val entNum = t.getCell(2).textContent.trim { it <= ' ' }
            var purNum = t.getCell(2).textContent.trim { it <= ' ' }
            val pattern: Pattern = Pattern.compile("\\s+")
            val matcher: Matcher = pattern.matcher(purNum)
            purNum = matcher.replaceAll("")
            val purObj = t.getCell(3).textContent.trim { it <= ' ' }
            val nmck = t.getCell(4).textContent.trim { it <= ' ' }
            val placingWay = t.getCell(0).textContent.trim { it <= ' ' }
            val datePubTmp = t.getCell(8).textContent.trim { it <= ' ' }
            val dateEndTmp = t.getCell(9).textContent.trim { it <= ' ' }
            var datePub = getDateFromFormat(datePubTmp, formatterOnlyDate)
            if (datePub == Date(0L)) {
                datePub = getDateFromFormat(datePubTmp, formatterEtpRf)
            }
            var dateEnd = getDateFromFormat(dateEndTmp, formatterEtpRfN)
            if (dateEnd == Date(0L)) {
                dateEnd = getDateFromFormat(dateEndTmp, formatterEtpRf)
            }
            if (dateEnd == Date(0L)) {
                dateEnd = dateAddHours(datePub, 48)
            }
            val urlT = t.getCell(11).getElementsByTagName("a")[0].getAttribute("href")
            if (urlT.contains("zakupki.butb.by")) return

            val url = if (urlT.startsWith("http://sale.etprf.ru")) {
                urlT
            } else {
                "$BaseUrl$urlT"
            }
            val tt = TenderEtpRf(status, entNum, purNum, purObj, nmck, placingWay, datePub, dateEnd, url)
            tt.parsing()
        } catch (e: Exception) {
            logger("error in ParserEtpRf.parserTender()", e.stackTrace, e)
        }

    }

    private fun parserTenderN(t: HtmlTableRow) {
        try {
            var status = t.getCell(8).textContent.trim { it <= ' ' }
            if (status.contains("ЕИС")) return
            val statusT = t.getCell(7).textContent.trim { it <= ' ' }
            if (statusT != "") status = statusT
            val entNum = t.getCell(0).textContent.trim { it <= ' ' }
            var purNum = t.getCell(1).textContent.trim { it <= ' ' }
            val pattern: Pattern = Pattern.compile("\\s+")
            val matcher: Matcher = pattern.matcher(purNum)
            purNum = matcher.replaceAll("")
            val purObj = t.getCell(2).textContent.trim { it <= ' ' }
            val nmck = t.getCell(3).textContent.trim { it <= ' ' }
            val placingWay = ""
            val datePubTmp = t.getCell(5).textContent.trim { it <= ' ' }
            val dateEndTmp = t.getCell(6).textContent.trim { it <= ' ' }
            val datePub = getDateFromFormat(datePubTmp, formatterEtpRfN)
            val dateEnd = getDateFromFormat(dateEndTmp, formatterEtpRfN)
            val urlT = t.getCell(9).getElementsByTagName("a")[0].getAttribute("href")
            if (urlT.contains("zakupki.butb.by")) return
            val url = "$BaseUrl$urlT"
            val tt = TenderEtpRf(status, entNum, purNum, purObj, nmck, placingWay, datePub, dateEnd, url)
            tt.parsing()
        } catch (e: Exception) {
            logger("error in ParserEtpRf.parserTenderN()", e.stackTrace, e)
        }
    }

}