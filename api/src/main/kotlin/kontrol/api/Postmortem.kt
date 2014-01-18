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

package kontrol.api

import kontrol.api.sensor.SensorValue
import java.io.Serializable

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public trait Postmortem {


    fun perform(machine: Machine): PostmortemResult

}

public trait PostmortemStore : Store<String, PostmortemResult> {

}

public open class PostmortemResult(var name: String? = null,
                                   var machine: Machine? = null,
                                   var parts: Map<String, PostmortemPart>? = null,

                                   var machineDataSnapshot: Map<String, SensorValue<Any?>>) : Serializable {
    fun toHTML(): String {
        val stringBuilder = StringBuilder("<h3>$name postmortem on ${machine?.name()}</h3>")
        val map = parts
        if (map != null) {
            map.map { "<div><h4>${it.key}</h4> ${it.value.toHTML()}</div>" }.appendString(stringBuilder)
        }
        return stringBuilder.toString();
    }

    override fun toString(): String {
        val stringBuilder = StringBuilder("$name postmortem on ${machine?.name()}\n")
        val map = parts
        if (map != null) {
            map.values().map { it.toString() }.appendString(stringBuilder)
        }
        return stringBuilder.toString();
    }

}

public trait PostmortemPart : Serializable {

    fun toHTML(): String {
        return toString()!!
    }
}