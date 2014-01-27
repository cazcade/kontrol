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
import kontrol.api.sensors.SensorArray
import kontrol.api.MonitorRule
import kontrol.api.Controller
import java.util.HashMap

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public class DigitalOceanMachineGroupMonitor(val group: DigitalOceanMachineGroup,
                                             val sensorArray: SensorArray,
                                             val controller: Controller) : Monitor<MachineGroupState, MachineGroup>{
    override fun target(): MachineGroup {
        return group;
    }


    override fun start(target: MachineGroup, stateMachine: StateMachine<MachineGroupState>, rules: Set<MonitorRule<MachineGroupState, MachineGroup>>) {
        controller.addGroupMonitor(this, group, rules)
    }


    override fun update() {
        try {
            val machines = HashMap<String, DigitalOceanMachine>();

            for (droplet in group.apiFactory.instance().droplets()) {
                if (droplet.name?.startsWith(group.config.machinePrefix + group.name)!!) {
                    val digitalOceanMachine = DigitalOceanMachine(droplet, group.apiFactory, sensorArray, controller, group.name)
                    machines.put(droplet.id.toString(), digitalOceanMachine)
                }

            }

            for (entry in machines) {
                if (!group.machines.containsKey(entry.key)) {
                    entry.value.fsm.rules = group.defaultMachineRules;
                    entry.value.startMonitoring(group.machineMonitorRules);
                    group.machines.putIfAbsent(entry.key, entry.value);
                }
            }
            group.machines.keySet().filterNot { machines.containsKey(it) }.forEach { group.machines.remove(it) }


        } catch (e: Throwable) {
            e.printStackTrace();
        }
    }
    override fun stop() {
        controller.removeGroupMonitor(this)
    }
}