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
import java.util.Timer
import kotlin.concurrent.*;
import kontrol.api.sensors.SensorArray
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.ConcurrentHashMap
import kontrol.api.sensor.SensorValue
import kontrol.api.MonitorRule

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public final class DigitalOceanMachine(var droplet: Droplet,
                                       val clientFactory: DigitalOceanClientFactory,

                                       val sensorArray: SensorArray<Any?>) : Machine {
    override var disableAction: ((Machine) -> Unit)? = null
    override var enableAction: ((Machine) -> Unit)? = null
    override final var data: ConcurrentMap<String, SensorValue<Any?>> = ConcurrentHashMap();
    override final var monitor: Monitor<MachineState, Machine> = DigitalOceanMachineMonitor(clientFactory);
    override final val stateMachine: StateMachine<MachineState, Machine> = DefaultStateMachine<MachineState, Machine>(this);
    override var enabled: Boolean = true;


    val timer = Timer("DO-" + id(), true);

    override fun id(): String {
        return droplet.id.toString();
    }


    override fun privateIp(): String? {
        return droplet.private_ip_address
    }

    override fun startMonitoring(rules: Set<MonitorRule<MachineState, Machine>>) {
        super<Machine>.startMonitoring(rules);
        timer.schedule((5000 * Math.random()).toLong(), 20000) {
            //            println("Updating ${droplet.getId()}")
            val doa = clientFactory.instance();
            droplet = doa.getDropletInfo(droplet.id?:-1) ?: droplet;
            if (state() in listOf(MachineState.BROKEN, MachineState.OK, MachineState.STARTING, MachineState.STALE)) {
                data.putAll(sensorArray.values(this@DigitalOceanMachine))
            } else {
                data.clear()
            }
        }

    }


    override fun stopMonitoring() {
        timer.cancel();
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
