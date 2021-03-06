/*
 * Copyright 2014 Cazcade Limited (http://cazcade.com)
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

package kontrol.sensor

import kontrol.api.Sensor
import kontrol.api.Machine
import kontrol.api.sensor.SensorValue
import java.net.URL
import java.util.Locale
import kontrol.HttpUtil
import java.net.URI

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public class HttpStatusSensor(val path: String, val port: Int = 80) : Sensor {

    override fun name(): String {
        return if (this.port == 80) "http-status" else ("http-status-" + port);
    }

    override fun value(machine: Machine): SensorValue {
        try {
            return SensorValue(name(), when {
                machine.hostname().isNotEmpty() -> {
                    val url: URI;
                    if (path.startsWith("http:") || path.startsWith("https:")) {
                        url = URL(path).toURI()

                    } else {
                        url = URL("http", machine.hostname(), port, path).toURI()

                    }
                    val status = HttpUtil.getStatus(url, Locale.getDefault(), 30 * 60 * 1000)
                    //                    println("url responded with $status")
                    status;

                }
                else -> {
                    null
                }
            });
        } catch (e: Exception) {
            println("HttpStatusSensor: ${e.javaClass} for ${machine.name()}")
            return SensorValue(name(), -1);
        }

    }
}