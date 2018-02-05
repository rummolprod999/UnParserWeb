import java.io.InputStreamReader
import java.io.BufferedReader
import java.io.InputStream
import java.lang.Thread.sleep
import java.net.URL
import java.util.concurrent.*


fun downloadFromUrl(urls: String): String {
    var count = 0
    while (true) {
        val i = 50
        if (count > i) {
            logger(String.format("Не скачали строку за %d попыток", count), urls)
            break
        }
        try {
            var s: String
            val executor = Executors.newCachedThreadPool()
            val task = { downloadWait(urls) }
            val future = executor.submit(task)
            try {
                s =  future.get(60, TimeUnit.SECONDS)
            } catch (ex: TimeoutException) {
                throw ex
            } catch (ex: InterruptedException) {
                throw ex
            } catch (ex: ExecutionException) {
                throw ex
            } finally {
                future.cancel(true)
                executor.shutdown()
            }
            return s

        } catch (e: Exception) {
            count++
            sleep(5000)
        }

    }
    return ""
}

fun downloadWait(urls: String): String {
    val s = StringBuilder()
    val url = URL(urls)
    val `is`: InputStream = url.openStream()
    val br = BufferedReader(InputStreamReader(`is`))
    var inputLine: String?
    var value = true
    while (value) {
        inputLine = br.readLine()
        if (inputLine == null) {
            value = false
        } else {
            s.append(inputLine)
        }

    }
    br.close()
    `is`.close()
    return s.toString()
}
