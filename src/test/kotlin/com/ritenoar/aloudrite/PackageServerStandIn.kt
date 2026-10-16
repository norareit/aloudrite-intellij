package com.ritenoar.aloudrite

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

// Plain HTTP on the loopback address, so that a test needs no network
class PackageServerStandIn : AutoCloseable {

    private val server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
    private val requestCounts = ConcurrentHashMap<String, AtomicInteger>()

    init {
        server.executor = Executors.newCachedThreadPool()
        server.start()
    }

    fun address(path: String) = "http://127.0.0.1:${server.address.port}$path"

    fun requestCount(path: String): Int = requestCounts[path]?.get() ?: 0

    fun serve(path: String, bytes: ByteArray) = route(path) { exchange ->
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    // Announces the whole length, then closes the connection after the given number of bytes
    fun serveCutOff(path: String, bytes: ByteArray, afterBytes: Int) = route(path) { exchange ->
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        exchange.responseBody.write(bytes, 0, afterBytes)
        exchange.responseBody.flush()
        exchange.close()
    }

    fun redirect(path: String, location: String, status: Int = 302) = route(path) { exchange ->
        exchange.responseHeaders.add("Location", location)
        exchange.sendResponseHeaders(status, -1)
        exchange.close()
    }

    fun answerWith(path: String, status: Int) = route(path) { exchange ->
        exchange.sendResponseHeaders(status, -1)
        exchange.close()
    }

    private fun route(path: String, respond: (HttpExchange) -> Unit) {
        server.createContext(path) { exchange ->
            requestCounts.computeIfAbsent(path) { AtomicInteger() }.incrementAndGet()
            try {
                respond(exchange)
            } catch (_: IOException) {
                // The client may hang up on purpose, as it does when it is cancelled
            } finally {
                exchange.close()
            }
        }
    }

    override fun close() {
        server.stop(0)
        (server.executor as java.util.concurrent.ExecutorService).shutdownNow()
    }
}
