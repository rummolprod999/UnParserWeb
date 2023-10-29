package enterit

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.Thread.sleep
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.net.ssl.*


var trustAllCerts: Array<TrustManager> = arrayOf<TrustManager>(
    object : X509TrustManager {

        override fun checkClientTrusted(
            certs: Array<X509Certificate?>?, authType: String?
        ) {
        }

        override fun checkServerTrusted(
            certs: Array<X509Certificate?>?, authType: String?
        ) {
        }

        override fun getAcceptedIssuers(): Array<X509Certificate>? {
            return null
        }
    }
)

fun downloadFromUrl(urls: String, i: Int = 5, wt: Long = 3000): String {
    try {
        val sc: SSLContext = SSLContext.getInstance("SSL")
        sc.init(null, trustAllCerts, SecureRandom())
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory())
        val allHostsValid: HostnameVerifier = object : HostnameVerifier {
            override fun verify(hostname: String?, session: SSLSession?): Boolean {
                return true
            }
        }
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
    } catch (e: Exception) {
    }
    var count = 0
    while (true) {
        //val i = 50
        if (count >= i) {
            logger(String.format("Не скачали строку за %d попыток", count), urls)
            break
        }
        try {
            var s: String
            val executor = Executors.newCachedThreadPool()
            val task = { downloadWaitWithRef(urls) }
            val future = executor.submit(task)
            try {
                s = future.get(60, TimeUnit.SECONDS)
            } catch (ex: TimeoutException) {
                throw ex
            } catch (ex: InterruptedException) {
                throw ex
            } catch (ex: ExecutionException) {
                throw ex
            } catch (ex: Exception) {
                throw ex
            } finally {
                future.cancel(true)
                executor.shutdown()
            }
            return s

        } catch (e: Exception) {
            logger(e, e.stackTrace)
            count++
            sleep(wt)
        }

    }
    return ""
}

fun downloadFromUrlEtpRf(urls: String, i: Int = 5, wt: Long = 3000): String {
    try {
        val sc: SSLContext = SSLContext.getInstance("SSL")
        sc.init(null, trustAllCerts, SecureRandom())
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory())
        val allHostsValid: HostnameVerifier = object : HostnameVerifier {
            override fun verify(hostname: String?, session: SSLSession?): Boolean {
                return true
            }
        }
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
    } catch (e: Exception) {
    }
    var count = 0
    while (true) {
        //val i = 50
        if (count >= i) {
            logger(String.format("Не скачали строку за %d попыток", count), urls)
            break
        }
        try {
            var s: String
            val executor = Executors.newCachedThreadPool()
            val task = { downloadWaitWithRefOkko(urls) }
            val future = executor.submit(task)
            try {
                s = future.get(60, TimeUnit.SECONDS)
            } catch (ex: TimeoutException) {
                throw ex
            } catch (ex: InterruptedException) {
                throw ex
            } catch (ex: ExecutionException) {
                throw ex
            } catch (ex: Exception) {
                throw ex
            } finally {
                future.cancel(true)
                executor.shutdown()
            }
            return s

        } catch (e: Exception) {
            logger(e, e.stackTrace)
            count++
            sleep(wt)
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

fun downloadWaitWithRef(urls: String): String {
    val s = StringBuilder()
    val url = URL(urls)
    val uc = url.openConnection()
    uc.connectTimeout = 30_000
    uc.readTimeout = 600_000
    uc.addRequestProperty("User-Agent", RandomUserAgent.randomUserAgent)
    uc.connect()
    val `is`: InputStream = uc.getInputStream()
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

fun downloadWaitWithRefEtprf(urls: String): String {
    val s = StringBuilder()
    val url = URL(urls)
    val uc = url.openConnection()
    uc.connectTimeout = 30_000
    uc.readTimeout = 600_000
    uc.addRequestProperty(
        "User-Agent",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36"
    )
    uc.addRequestProperty("Cookie", Cookies)
    uc.connect()
    val `is`: InputStream = uc.getInputStream()
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

fun downloadWaitWithRefCurl(urls: String): String {
    val commands = "curl -k \"$urls\" \\\n" +
            "  -H \"Cookie: $Cookies\" \\\n" +
            "  --compressed"
    val process = Runtime.getRuntime().exec(commands)
    val stdInput = process.errorStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    return stdInput
}

fun downloadWaitWithRefOkko(urls: String): String {
    val TRUST_ALL_CERTS: TrustManager = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate?>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<X509Certificate?>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> {
            return arrayOf()
        }
    }
    val sslContext = SSLContext.getInstance("SSL")
    sslContext.init(null, arrayOf(TRUST_ALL_CERTS), SecureRandom())
    val client =
        OkHttpClient.Builder().sslSocketFactory(sslContext.socketFactory, TRUST_ALL_CERTS as X509TrustManager).build()

    val request = Request.Builder()
        .url(urls)
        .header("Cookie", Cookies!!)
        .build()
    var resp = ""
    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw IOException("Unexpected code $response")
        resp = response.body!!.string()
    }
    return resp
}


fun downloadFromUrl1251(urls: String, i: Int = 5): String {
    var count = 0
    while (true) {
        //val i = 50
        if (count >= i) {
            logger(String.format("Не скачали строку за %d попыток", count), urls)
            break
        }
        try {
            var s: String
            val executor = Executors.newCachedThreadPool()
            val task = { downloadWaitWithRef1251(urls) }
            val future = executor.submit(task)
            try {
                s = future.get(60, TimeUnit.SECONDS)
            } catch (ex: TimeoutException) {
                throw ex
            } catch (ex: InterruptedException) {
                throw ex
            } catch (ex: ExecutionException) {
                throw ex
            } catch (ex: Exception) {
                throw ex
            } finally {
                future.cancel(true)
                executor.shutdown()
            }
            return s

        } catch (e: Exception) {
            logger(e, e.stackTrace)
            count++
            sleep(5000)
        }

    }
    return ""
}

fun downloadWaitWithRef1251(urls: String): String {
    val s = StringBuilder()
    val url = URL(urls)
    val uc = url.openConnection()
    uc.connectTimeout = 30_000
    uc.readTimeout = 600_000
    uc.addRequestProperty(
        "User-Agent",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/104.0.0.0 Safari/537.36"
    )
    uc.connect()
    val `is`: InputStream = uc.getInputStream()
    val br = BufferedReader(InputStreamReader(`is`, "windows-1251"))
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