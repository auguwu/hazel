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

@file:Suppress("UNUSED")
package dev.floofy.hazel.routing.endpoints

import dev.floofy.hazel.HazelInfo
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.noelware.ktor.endpoints.AbstractEndpoint
import org.noelware.ktor.endpoints.Get

class InfoEndpoint: AbstractEndpoint("/info") {
    @Get
    suspend fun call(call: ApplicationCall) {
        call.respond(
            HttpStatusCode.OK,
            buildJsonObject {
                put("success", true)
                put(
                    "data",
                    buildJsonObject {
                        put("version", HazelInfo.version)
                        put("commit_sha", HazelInfo.commitHash)
                        put("build_date", HazelInfo.buildDate)

                        if (HazelInfo.dediNode != null)
                            put("dedi_node", HazelInfo.dediNode)
                    }
                )
            }
        )
    }
}
