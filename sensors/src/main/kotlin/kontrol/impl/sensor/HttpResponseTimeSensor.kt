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

import kontrol.api.Machine
import kontrol.api.sensor.SensorValue
import java.net.URL
import kontrol.HttpUtil
import kontrol.api.Sensor
import java.util.Locale

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public class HttpResponseTimeSensor(val path: String, val port: Int = 80) : Sensor {

    override fun name(): String {
        return "http-response-time"
    }

    override fun value(machine: Machine): SensorValue {
        try {
            return SensorValue(name(), when {
                machine.hostname().isNotEmpty() -> {

                    val start = System.currentTimeMillis();
                    val URI = URL("http", machine.hostname(), port, path).toURI()
                    HttpUtil.getStatus(URI, Locale.getDefault(), 10 * 1000)
                    System.currentTimeMillis() - start

                }
                else -> {
                    null
                }
            });
        } catch (e: Exception) {
            println("HttpResponseTimeSensor: ${e.javaClass} for ${machine.name()}")
            return SensorValue(name(), null);
        }

    }
}