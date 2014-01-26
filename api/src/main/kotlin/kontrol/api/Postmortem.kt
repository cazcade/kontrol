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
import javax.persistence.Entity as entity
import javax.persistence.Id as id
import javax.persistence.GeneratedValue as generated
import javax.persistence.Transient as transient
import javax.persistence.*
import java.util.Date
import java.util.UUID
import java.util.HashSet

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public trait Postmortem {


    fun perform(machine: Machine): PostmortemResult

}

public trait PostmortemStore : TableStore<Int, PostmortemResult> {
    fun getWithParts(key: Int): PostmortemResult?


}

public entity data class PostmortemResult(var name: String? = null,
                                          transient var machine: Machine? = null,
                                          OneToMany(cascade = array(CascadeType.ALL)) var parts: MutableList<PostmortemPart>? = null,
                                          transient  var machineDataSnapshot: MutableMap<String, SensorValue?>? = null,
                                          var created: Date? = Date()) : Serializable {

    id generated var id: Int? = null
    var machineName: String? = null;
    var machineIp: String? = null;
    var machineHostname: String? = null;
    var machineState: String? = null;
    OneToMany(cascade = array(CascadeType.ALL))  var sensors: MutableSet<SensorValue>? = HashSet()   ;

    {
        machineName = machine?.name()
        machineIp = machine?.ip()
        machineHostname = machine?.hostname()
        machineState = machine?.state().toString()
        (machineDataSnapshot?.values()?:listOf<SensorValue?>()).filterNotNull().forEach { sensors?.add(it.withNewId(UUID.randomUUID().toString())) }
    }

    fun toHTML(): String {
        val stringBuilder = StringBuilder("<h3>$name postmortem on ${machineName}</h3>")
        val list = parts
        if (list != null) {
            list.map { "<div><h4>${it.k}</h4> ${it.toHTML()}</div>" }.appendString(stringBuilder)
        }
        return stringBuilder.toString();
    }

    override fun toString(): String {
        val stringBuilder = StringBuilder("$name postmortem on ${machineName}\n")
        val list = parts
        if (list != null) {
            list.map { it.toString() }.appendString(stringBuilder)
        }
        return stringBuilder.toString();
    }

}

public open entity data class PostmortemPart(public var k: String? = null, public Lob var v: String? = null, public id var id: String? = null) : Serializable {

    {
        id = UUID.randomUUID().toString()
    }


    open fun toHTML(): String {
        return toString()!!
    }
}