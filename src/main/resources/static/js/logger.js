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

class Logger {
  info(message) {
    console.info(`[INFO] ${message}`);
  }

  error(message, error) {
    if (!error) {
      console.error(`[ERROR] ${message}`);
    } else {
      console.error(`[ERROR] ${message}\n${error}`);
    }
  }

  warn(message) {
    console.warn(`[WARN] ${message}`);
  }
}

window.$log = new Logger();
