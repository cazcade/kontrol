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

package kontrol.digitalocean

import kontrol.api.Machine
import kontrol.api.MachineState
import kontrol.api.Monitor
import kontrol.api.StateMachine
import kontrol.common.DefaultStateMachine
import kontrol.api.sensors.SensorArray
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.ConcurrentHashMap
import kontrol.api.sensor.SensorValue
import kontrol.api.MonitorRule
import kontrol.api.Controller
import kontrol.api.ComparableTemporalStore
import kontrol.staticmc.StaticMachineMonitor
import kontrol.api.Monitorable

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public final class StaticMachine(val sensorArray: SensorArray, val controller: Controller, val groupName: String, val cost: Double, val ipAddress: String, val privateIpAddress: String = ipAddress, val hostname: String = ipAddress, val id: String = ipAddress) : Machine {


    override fun costPerHourInDollars(): Double {
        return cost;
    }

    override fun groupName(): String {
        return groupName
    }
    override var disableAction: ((Monitorable<MachineState>) -> Unit)? = null
    override var enableAction: ((Monitorable<MachineState>) -> Unit)? = null
    override final var data: ConcurrentMap<String, ComparableTemporalStore<SensorValue>> = ConcurrentHashMap();
    override final val fsm: StateMachine<MachineState> = DefaultStateMachine<MachineState>(this);
    override final var monitor: Monitor<MachineState, Machine> = StaticMachineMonitor(this, fsm, controller);
    override var enabled: Boolean = true;


    override fun id(): String {
        return id
    }


    override fun hostname(): String {
        return hostname
    }
    override fun privateIp(): String? {
        return privateIpAddress
    }

    override fun startMonitoring(rules: Set<MonitorRule<MachineState, Machine>>) {
        super<Machine>.startMonitoring(rules);

    }


    override fun stopMonitoring() {
        super<Machine>.stopMonitoring()
    }

    override fun ip(): String? {
        return ipAddress;
    }


    override fun name(): String {
        return "${hostname()} (${id()})"
    }


}
