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

import kontrol.api.Monitor
import kontrol.api.MachineGroupState
import kontrol.api.MachineGroup
import kontrol.api.StateMachine
import java.util.Timer
import kotlin.concurrent.*;
import java.util.HashMap
import kontrol.api.sensors.SensorArray
import kontrol.api.MonitorRule

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public class DigitalOceanMachineGroupMonitor(val group: DigitalOceanMachineGroup, val sensorArray: SensorArray<Any?>) : Monitor<MachineGroupState, MachineGroup>{

    val timer = Timer("DOGroupMon", true);

    override fun start(target: MachineGroup, stateMachine: StateMachine<MachineGroupState, MachineGroup>, rules: Set<MonitorRule<MachineGroupState, MachineGroup>>) {

        timer.schedule(1000, 5000) {
            rules.forEach { it.evaluate(target) }
        }

        timer.schedule(100, 30000) {
            try {
                val machines = HashMap<String, DigitalOceanMachine>();

                for (droplet in group.apiFactory.instance().getAvailableDroplets()) {
                    if (droplet.name?.startsWith(group.config.machinePrefix + group.name)!!) {
                        val digitalOceanMachine = DigitalOceanMachine(droplet, group.apiFactory, sensorArray)
                        machines.put(droplet.id.toString(), digitalOceanMachine)
                    }

                }

                synchronized(group.machines) {
                    for (entry in machines) {
                        if (!group.machines.containsKey(entry.key)) {
                            entry.value.stateMachine.rules = group.defaultMachineRules;
                            entry.value.startMonitoring(group.machineMonitorRules);
                            group.machines.put(entry.key, entry.value);
                        }
                    }
                    for (machine in group.machines) {
                        if (!machines.containsKey(machine.key)) {
                            group.machines.remove(machine.key)?.stopMonitoring();
                        }
                    }
                }


            } catch (e: Throwable) {
                e.printStackTrace();
            }
        }

    }
    override fun stop() {
        timer.cancel();
    }
}