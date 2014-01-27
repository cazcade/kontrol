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
import kontrol.doclient.Droplet
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

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public final class DigitalOceanMachine(var droplet: Droplet,
                                       val clientFactory: DigitalOceanClientFactory,
                                       val sensorArray: SensorArray, val controller: Controller, val groupName: String) : Machine {

    var pricingMap = hashMapOf(
            66 to 0.007,
            63 to 0.015,
            62 to 0.03,
            64 to 0.06,
            65 to 0.119,
            61 to 0.238,
            60 to 0.476,
            70 to 0.705,
            69 to 0.941,
            68 to 1.411
    )

    override fun costPerHourInDollars(): Double {
        return pricingMap[droplet.size_id]?:throw IllegalArgumentException();
    }

    override fun groupName(): String {
        return groupName
    }
    override var disableAction: ((Machine) -> Unit)? = null
    override var enableAction: ((Machine) -> Unit)? = null
    override final var data: ConcurrentMap<String, ComparableTemporalStore<SensorValue>> = ConcurrentHashMap();
    override final val fsm: StateMachine<MachineState> = DefaultStateMachine<MachineState>(this);
    override final var monitor: Monitor<MachineState, Machine> = DigitalOceanMachineMonitor(clientFactory, this, fsm, controller);
    override var enabled: Boolean = true;


    override fun id(): String {
        return droplet.id.toString();
    }


    override fun privateIp(): String? {
        return droplet.private_ip_address
    }

    override fun startMonitoring(rules: Set<MonitorRule<MachineState, Machine>>) {
        super<Machine>.startMonitoring(rules);

    }


    override fun stopMonitoring() {
        super<Machine>.stopMonitoring()
    }

    override fun ip(): String? {
        val ip = droplet.ip_address
        return ip;
    }


    override fun name(): String {
        return "${droplet.name} (${droplet.id})"
    }


}
