package enterit.tenders

import enterit.Prefix
import enterit.getConformity
import java.sql.Connection
import java.sql.Statement

abstract class TenderAbstract {
    var etpName = ""
    var etpUrl = ""
    fun getEtp(con: Connection): Int {
        var IdEtp = 0
        val stmto = con.prepareStatement("SELECT id_etp FROM ${Prefix}etp WHERE name = ? AND url = ? LIMIT 1")
        stmto.setString(1, etpName)
        stmto.setString(2, etpUrl)
        val rso = stmto.executeQuery()
        if (rso.next()) {
            IdEtp = rso.getInt(1)
            rso.close()
            stmto.close()
        } else {
            rso.close()
            stmto.close()
            val stmtins = con.prepareStatement("INSERT INTO ${Prefix}etp SET name = ?, url = ?, conf=0", Statement.RETURN_GENERATED_KEYS)
            stmtins.setString(1, etpName)
            stmtins.setString(2, etpUrl)
            stmtins.executeUpdate()
            val rsoi = stmtins.generatedKeys
            if (rsoi.next()) {
                IdEtp = rsoi.getInt(1)
            }
            rsoi.close()
            stmtins.close()
        }
        return IdEtp
    }

    fun getPlacingWay(con: Connection, placingWay: String): Int {
        var idPlacingWay = 0
        val stmto = con.prepareStatement("SELECT id_placing_way FROM ${Prefix}placing_way WHERE name = ? LIMIT 1")
        stmto.setString(1, placingWay)
        val rso = stmto.executeQuery()
        if (rso.next()) {
            idPlacingWay = rso.getInt(1)
            rso.close()
            stmto.close()
        } else {
            rso.close()
            stmto.close()
            val conf = getConformity(placingWay)
            val stmtins = con.prepareStatement("INSERT INTO ${Prefix}placing_way SET name = ?, conformity = ?", Statement.RETURN_GENERATED_KEYS)
            stmtins.setString(1, placingWay)
            stmtins.setInt(2, conf)
            stmtins.executeUpdate()
            val rsoi = stmtins.generatedKeys
            if (rsoi.next()) {
                idPlacingWay = rsoi.getInt(1)
            }
            rsoi.close()
            stmtins.close()

        }
        return idPlacingWay
    }
}