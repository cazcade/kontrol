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
import kontrol.api.LoadSensor
import kontrol.HttpUtil

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public class HttpLoadSensor(val path: String) : LoadSensor {

    override fun name(): String {
        return "http-load"
    }

    override fun value(machine: Machine): SensorValue {
        try {
            return SensorValue(name(), when {
                machine.hostname().isNotEmpty() -> {

                    val URI = URL("http", machine.hostname(), 80, path).toURI()
                    val loadStr = HttpUtil.getUrlAsString(URI, 10)
                    if (loadStr == "disabled") {
                        0.0;
                    } else {
                        val load = loadStr?.toDouble()
                        //                    println("$URI responded with $load")
                        load;
                    }

                }
                else -> {
                    null
                }
            });
        } catch (e: Exception) {
            println("HttpLoadSensor: ${e.javaClass} for ${machine.name()}")
            return SensorValue(name(), null);
        }

    }
}