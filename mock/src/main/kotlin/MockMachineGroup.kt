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

package kontrol.mock

import kontrol.api.MachineGroup
import kontrol.api.Machine
import kontrol.api.MachineState
import kontrol.common.DefaultStateMachineRules
import kontrol.api.MachineGroupState
import kontrol.common.DefaultStateMachine
import kontrol.api.Monitor
import kontrol.api.sensors.SensorArray
import kontrol.common.DefaultSensorArray
import java.util.ArrayList
import kontrol.api.MonitorRule
import kontrol.api.DownStreamKonfigurator
import kontrol.api.UpStreamKonfigurator
import java.util.SortedSet
import java.util.TreeSet
import kontrol.api.Postmortem
import kontrol.api.Controller
import kontrol.api.Monitorable
import kontrol.api.sensors.GroupSensorArray
import kontrol.common.DefaultGroupSensorArray

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public class MockMachineGroup(val name: String, val machines: MutableList<MockMachine>, override val monitor: Monitor<MachineGroupState, MachineGroup>, override val upstreamGroups: MutableList<MachineGroup>, override val downStreamKonfigurator: DownStreamKonfigurator?, override val upStreamKonfigurator: UpStreamKonfigurator?, override val controller: Controller, override val groupSensors: GroupSensorArray = DefaultGroupSensorArray()) : MachineGroup{
    override var disableAction: ((Monitorable<MachineGroupState>) -> Unit)? = null
    override var enableAction: ((Monitorable<MachineGroupState>) -> Unit)? = null
    override val hardMax: Int = 10000000
    override fun costPerHourInDollars(): Double {
        return 0.0;
    }

    override fun groupName(): String {
        return name;
    }
    override val postmortems: List<Postmortem> = listOf()
    override val downStreamGroups: MutableList<MachineGroup> = ArrayList()


    override var enabled: Boolean = true
    override val min: Int = 0
    override val max: Int = 100000
    var configureCalls: Int = 0

    override val machineMonitorRules: SortedSet<MonitorRule<MachineState, Machine>> = TreeSet();
    override val groupMonitorRules: SortedSet<MonitorRule<MachineGroupState, MachineGroup>> = TreeSet();
    override val sensors: SensorArray = DefaultSensorArray(ArrayList())
    override val stateMachine = DefaultStateMachine<MachineGroupState>(this);
    override val defaultMachineRules = DefaultStateMachineRules<MachineState>();

    {
        machines.forEach { it.fsm.rules = defaultMachineRules }
        stateMachine.rules = DefaultStateMachineRules<MachineGroupState>();
        println("${name()} has upstream group ${upstreamGroups}")
        upstreamGroups.forEach { (it as MockMachineGroup).downStreamGroups.add(this) }

    }

    override fun name(): String {
        return name;
    }

    override fun machines(): List<Machine> {
        return machines;
    }

    override fun expand(): Machine {
        println("**** Expand $name");
        val mockMachine = MockMachine("10.10.10." + (Math.random() * 256));
        mockMachine.monitor = MockMachineMonitor();
        mockMachine.fsm.rules = defaultMachineRules;
        machines.add(mockMachine);
        return mockMachine;
    }

    override fun contract(): MachineGroup {
        println("**** Contract $name");
        if (machines.size() > 0) {
            machines.remove(machines[0]);
        }
        return this;
    }


    override fun destroy(machine: Machine): MachineGroup {
        machines.remove(machine);
        return this
    }


    override fun configure(): MachineGroup {
        this.configureCalls++;
        return super<MachineGroup>.configure()
    }
}