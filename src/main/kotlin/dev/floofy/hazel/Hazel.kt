/*
 * 🪶 hazel: Minimal, simple, and open source content delivery network made in Kotlin
 * Copyright 2022 Noel <cutie@floofy.dev>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.floofy.hazel

import com.github.mustachejava.DefaultMustacheFactory
import dev.floofy.hazel.core.Ticker
import dev.floofy.hazel.core.createThreadFactory
import dev.floofy.hazel.data.Config
import dev.floofy.hazel.extensions.resourcePath
import dev.floofy.hazel.plugins.KtorLoggingPlugin
import dev.floofy.hazel.plugins.UserAgentPlugin
import dev.floofy.hazel.routing.createCdnEndpoints
import dev.floofy.utils.koin.*
import dev.floofy.utils.kotlin.*
import dev.floofy.utils.slf4j.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.mustache.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.sentry.Sentry
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.koin.core.context.GlobalContext
import org.noelware.ktor.NoelKtorRoutingPlugin
import org.noelware.ktor.loader.koin.KoinEndpointLoader
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.minutes

class Hazel {
    companion object {
        val executorPool: ExecutorService = Executors.newCachedThreadPool(createThreadFactory("ExecutorThreadPool"))
    }

    private val routesRegistered = mutableListOf<Pair<HttpMethod, String>>()
    lateinit var server: NettyApplicationEngine
    private val log by logging<Hazel>()

    suspend fun start() {
        val runtime = Runtime.getRuntime()
        val os = ManagementFactory.getOperatingSystemMXBean()
        val threads = ManagementFactory.getThreadMXBean()

        log.info("+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+")
        log.info("Runtime Information:")
        log.info("  * Free / Total Memory [Max]: ${runtime.freeMemory().sizeToStr()}/${runtime.totalMemory().sizeToStr()} [${runtime.maxMemory().sizeToStr()}]")
        log.info("  * Threads: ${threads.threadCount} (${threads.daemonThreadCount} background threads)")
        log.info("  * Operating System: ${os.name} with ${os.availableProcessors} processors (${os.arch}; ${os.version})")
        log.info("  * Versions:")
        log.info("      * JVM [JRE]: v${System.getProperty("java.version", "Unknown")} (${System.getProperty("java.vendor", "Unknown")}) [${Runtime.version()}]")
        log.info("      * Kotlin:    v${KotlinVersion.CURRENT}")
        log.info("      * Hazel:     v${HazelInfo.version} (${HazelInfo.commitHash} -- ${HazelInfo.buildDate})")

        if (HazelInfo.dediNode != null)
            log.info("  * Dedicated Node: ${HazelInfo.dediNode}")

        log.info("+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+")

        val config: Config by inject()
        val self = this
        val ticker = Ticker("update image routing", 1.minutes.inWholeMilliseconds)

        ticker.launch {
            log.debug("Updating routes...")

            val routing = server.application.plugin(Routing)
            routing.createCdnEndpoints()
        }

        val environment = applicationEngineEnvironment {
            developmentMode = System.getProperty("dev.floofy.hazel.debug", "false") == "true"
            log = LoggerFactory.getLogger("dev.floofy.hazel.ktor.KtorApplicationEnvironment")

            connector {
                host = config.server.host
                port = config.server.port.toInt()
            }

            module {
                install(AutoHeadResponse)
                install(KtorLoggingPlugin)
                install(UserAgentPlugin)
                install(ContentNegotiation) {
                    json(GlobalContext.retrieve())

                    if (config.frontend) {
                        ignoreType<MustacheContent>()
                    }
                }

                install(CORS) {
                    anyHost()
                    headers += "X-Forwarded-Proto"
                }

                install(DefaultHeaders) {
                    header("X-Powered-By", "Noel/Hazel (+https://github.com/auguwu/hazel; v${HazelInfo.version})")
                    header("Cache-Control", "public, max-age=7776000")

                    if (config.server.securityHeaders) {
                        header("X-Frame-Options", "deny")
                        header("X-Content-Type-Options", "nosniff")
                        header("X-XSS-Protection", "1; mode=block")
                    }

                    for ((key, value) in config.server.extraHeaders) {
                        header(key, value)
                    }
                }

                install(StatusPages) {
                    // This is used if there is no content length (since Hazel sets
                    // it in the outgoing content)
                    statuses[HttpStatusCode.NotFound] = { call, content, _ ->
                        if (content.contentLength == null) {
                            call.respond(
                                HttpStatusCode.NotFound,
                                buildJsonObject {
                                    put("success", false)
                                    put(
                                        "errors",
                                        buildJsonArray {
                                            add(
                                                buildJsonObject {
                                                    put("code", "UNKNOWN_ROUTE")
                                                    put("message", "Route ${call.request.httpMethod.value} ${call.request.uri} was not found.")
                                                }
                                            )
                                        }
                                    )
                                }
                            )
                        }
                    }

                    // If the route has a different method handler
                    status(HttpStatusCode.MethodNotAllowed) { call, _ ->
                        call.respond(
                            HttpStatusCode.NotFound,
                            buildJsonObject {
                                put("success", false)
                                put(
                                    "errors",
                                    buildJsonArray {
                                        add(
                                            buildJsonObject {
                                                put("message", "Route ${call.request.httpMethod.value} ${call.request.uri} doesn't have a method handler.")
                                                put("code", "UNKNOWN_ROUTE")
                                            }
                                        )
                                    }
                                )
                            }
                        )
                    }

                    // General exception that we can swallow
                    exception<Exception> { call, cause ->
                        if (Sentry.isEnabled()) {
                            Sentry.captureException(cause)
                        }

                        self.log.error("Unable to handle request ${call.request.httpMethod.value} ${call.request.uri}:", cause)
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            buildJsonObject {
                                put("success", false)
                                put(
                                    "errors",
                                    buildJsonArray {
                                        add(
                                            buildJsonObject {
                                                put("message", "Unknown exception has occurred")
                                                put("code", "INTERNAL_SERVER_ERROR")
                                            }
                                        )
                                    }
                                )
                            }
                        )
                    }
                }

                routing {
                    if (config.frontend) {
                        resourcePath("/static/css/hazel.css")
                        resourcePath("/static/css/normalize.css")
                        resourcePath("/static/js/logger.js")
                        resourcePath("/static/js/main.js")
                        resourcePath("/static/js/sessions.js")
                        resourcePath("/static/js/token.js")
                    }

                    runBlocking {
                        createCdnEndpoints()
                    }
                }

                install(NoelKtorRoutingPlugin) {
                    endpointLoader = KoinEndpointLoader
                }

                if (config.frontend) {
                    log.info("Frontend is enabled! Enabling Mustache plugin...")

                    install(Mustache) {
                        mustacheFactory = DefaultMustacheFactory("templates")
                    }
                }
            }
        }

        server = embeddedServer(Netty, environment, configure = {
            requestQueueLimit = config.server.requestQueueLimit.toInt()
            runningLimit = config.server.runningLimit.toInt()
            shareWorkGroup = config.server.shareWorkGroup
            responseWriteTimeoutSeconds = config.server.responseWriteTimeoutSeconds.toInt()
            requestReadTimeoutSeconds = config.server.requestReadTimeout.toInt()
            tcpKeepAlive = config.server.tcpKeepAlive
        })

        if (!config.server.securityHeaders)
            log.warn("It is not recommended disabling security headers~")

        server.start(wait = true)
    }

    fun destroy() {
        if (!::server.isInitialized) return

        log.warn("Destroying API server...")
        server.stop(1000, 5000)
    }
}
