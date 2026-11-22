package com.ritenoar.aloudrite

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

// What the stand-in answers to one request, after the delay
class StandInAnswer(
    val status: Int = 200,
    val headers: Map<String, String> = emptyMap(),
    val body: ByteArray = ByteArray(0),
    val delayMillis: Long = 0,
)

class RecordedRequest(val rawPath: String, val atMillis: Long)

// Plain HTTP on the loopback address, so that a test needs no network. The clock stamps the requests it records, which a
// test that pauses on a clock of its own hands in
class PackageServerStandIn(private val clock: () -> Long = System::currentTimeMillis) : AutoCloseable {

    private val server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
    private val requestCounts = ConcurrentHashMap<String, AtomicInteger>()
    private val rawPathCounts = ConcurrentHashMap<String, AtomicInteger>()
    private val recordedRequests = CopyOnWriteArrayList<RecordedRequest>()
    private val openRequests = AtomicInteger()
    private val mostOpenRequests = AtomicInteger()

    // Every request, in the order the stand-in received them, with the raw path as it was sent
    val requests: List<RecordedRequest> get() = recordedRequests.toList()

    // The most requests that were open at the same moment, from their arrival to the end of their answer
    val mostRequestsOpenAtOnce: Int get() = mostOpenRequests.get()

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

    // Answers every request whose path starts with the prefix by the answer that is given for its raw path and for the
    // number of requests with that raw path so far, this one included. No answer means 404
    fun serveBy(prefix: String, answerFor: (rawPath: String, requestNumber: Int) -> StandInAnswer?) = route(prefix) { exchange ->
        val rawPath = exchange.requestURI.rawPath
        val requestNumber = rawPathCounts.computeIfAbsent(rawPath) { AtomicInteger() }.incrementAndGet()
        val answer = answerFor(rawPath, requestNumber) ?: StandInAnswer(status = 404)
        if (answer.delayMillis > 0) Thread.sleep(answer.delayMillis)
        answer.headers.forEach { (name, value) -> exchange.responseHeaders.add(name, value) }
        if (answer.body.isEmpty()) {
            exchange.sendResponseHeaders(answer.status, -1)
        } else {
            exchange.sendResponseHeaders(answer.status, answer.body.size.toLong())
            exchange.responseBody.use { it.write(answer.body) }
        }
    }

    private fun route(path: String, respond: (HttpExchange) -> Unit) {
        server.createContext(path) { exchange ->
            requestCounts.computeIfAbsent(path) { AtomicInteger() }.incrementAndGet()
            recordedRequests += RecordedRequest(exchange.requestURI.rawPath, clock())
            mostOpenRequests.accumulateAndGet(openRequests.incrementAndGet(), ::maxOf)
            try {
                respond(exchange)
            } catch (_: IOException) {
                // The client may hang up on purpose, as it does when it is cancelled
            } finally {
                openRequests.decrementAndGet()
                exchange.close()
            }
        }
    }

    override fun close() {
        server.stop(0)
        (server.executor as java.util.concurrent.ExecutorService).shutdownNow()
    }
}
