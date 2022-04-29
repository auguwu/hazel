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

package dev.floofy.hazel.routing.endpoints

import dev.floofy.hazel.routing.AbstractEndpoint
import org.koin.dsl.bind
import org.koin.dsl.module

val endpointsModule = module {
    single { ListFilesEndpoint(get(), get()) } bind AbstractEndpoint::class
    single { HeartbeatEndpoint() } bind AbstractEndpoint::class
    single { FaviconEndpoint() } bind AbstractEndpoint::class
    single { InfoEndpoint() } bind AbstractEndpoint::class
    single { MainEndpoint() } bind AbstractEndpoint::class
}