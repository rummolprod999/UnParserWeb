package enterit.tenders

import enterit.*
import java.sql.*
import java.util.Date

class TenderEtpRf(val status: String, val entNum: String, val purNum: String, val purObj: String, val nmck: String, val placingWay: String, val datePub: Date, val dateEnd: Date) {
    companion object TypeFz {
        val typeFz = 12
    }
    fun parsing(){
        DriverManager.getConnection(UrlConnect, UserDb, PassDb).use(fun(con: Connection) {

        })
    }
}