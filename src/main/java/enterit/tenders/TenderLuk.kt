package enterit.tenders

import com.gargoylesoftware.htmlunit.html.HtmlDivision

class TenderLuk(private val p: HtmlDivision) {
    fun parser() {
        var purObj = ""
        val purObjA = p.getElementsByTagName("h2")
        if (!purObjA.isEmpty()) {
            purObj = purObjA[0].textContent.trim { it <= ' ' }
        }
        println(purObj)
    }
}