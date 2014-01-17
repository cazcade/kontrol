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


/**
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 * @todo document.
 */
public trait Topology {

    val members: Map<String, MachineGroup>

    fun members(): Map<String, MachineGroup> {
        return members;
    }

    fun start() {
        println("Started Toplogy")
        each { it.startMonitoring() }
    }

    fun stop() {
        each { it.stopMonitoring() }
    }

    fun each(action: (MachineGroup) -> Unit): Topology {
        members().values().forEach { action(it) };
        return this;
    }

    fun eachMachine(action: (Machine) -> Unit): Topology {
        members().values().forEach { it.machines().forEach { action(it) } };
        return this;
    }

    fun get(name: String): MachineGroup {
        val machineGroup = members().get(name)
        return if (machineGroup != null) {
            machineGroup
        } else {
            throw  IllegalArgumentException("No such group $name")
        }
    }

    fun toString(): String {
        var string: String = "Toplogy: \n";
        for (member in members) {
            string += "${member.value}\n"
        }
        return string;
    }


}
