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

package dev.floofy.hazel.core

import dev.floofy.hazel.data.StorageClass
import dev.floofy.hazel.data.StorageConfig
import dev.floofy.utils.slf4j.*
import kotlinx.coroutines.runBlocking
import org.noelware.remi.core.StorageTrailer
import org.noelware.remi.core.figureContentType
import org.noelware.remi.filesystem.FilesystemStorageTrailer
import org.noelware.remi.minio.MinIOStorageTrailer
import org.noelware.remi.s3.S3StorageTrailer
import java.io.InputStream

/**
 * Wrapper for configuring the storage trailer that **hazel** will use.
 */
class StorageWrapper(config: StorageConfig) {
    val trailer: StorageTrailer<*>
    private val log by logging<StorageWrapper>()
    val listCache = mutableListOf<org.noelware.remi.core.Object>()

    init {
        log.info("Figuring out what storage trailer to use...")

        trailer = when (config.storageClass) {
            StorageClass.FS -> {
                assert(config.fs != null) { "Configuration for the local disk is missing." }

                FilesystemStorageTrailer(config.fs!!.directory)
            }

            StorageClass.FILESYSTEM -> {
                assert(config.filesystem != null) { "Configuration for the local disk is missing." }

                FilesystemStorageTrailer(config.filesystem!!.directory)
            }

            StorageClass.S3 -> {
                assert(config.s3 != null) { "Configuration for Amazon S3 is missing." }

                S3StorageTrailer(config.s3!!)
            }

            StorageClass.MINIO -> {
                assert(config.minio != null) { "Configuration for MinIO is missing." }

                MinIOStorageTrailer(config.minio!!)
            }
        }

        log.info("Using storage trailer ${config.storageClass}!")

        // block the main thread so the trailer can be
        // loaded successfully.
        runBlocking {
            try {
                log.info("Starting up storage trailer...")
                trailer.init()
            } catch (e: Exception) {
                if (e is IllegalStateException && e.message?.contains("doesn't support StorageTrailer#init/0") == true)
                    return@runBlocking

                throw e
            }
        }
    }

    /**
     * Opens a file under the [path] and returns the [InputStream] of the file.
     */
    suspend fun open(path: String): InputStream? = trailer.open(path)

    suspend fun listAll(force: Boolean = true): List<org.noelware.remi.core.Object> =
        if (force || listCache.isEmpty()) {
            val c = trailer.listAll()
            listCache.clear()
            listCache += c

            listCache
        } else {
            listCache
        }

    fun <I: InputStream> findContentType(stream: I): String = trailer.figureContentType(stream)
}
