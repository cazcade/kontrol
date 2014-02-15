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

package kontrol.staticmc

import kontrol.api.MachineGroup
import kontrol.api.Machine
import kontrol.api.MachineState
import kontrol.api.MachineGroupState
import kontrol.api.Monitor
import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap
import kontrol.api.sensors.SensorArray
import kontrol.api.MonitorRule
import kontrol.api.DownStreamKonfigurator
import kontrol.api.UpStreamKonfigurator
import java.util.SortedSet
import java.util.TreeSet
import kontrol.api.Postmortem
import kontrol.api.Controller
import kontrol.common.*
import kontrol.ext.string.ssh.onHost
import kontrol.digitalocean.StaticMachine
import kontrol.staticmnc.StaticMachineGroupMonitor

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public class StaticMachineGroup(members: List<StaticMachine>,
                                val restartAction: String = "reboot",
                                val rebuildAction: String = "reboot",
                                val user: String = "root",
                                public override val controller: Controller,
                                val name: String,
                                override val sensors: SensorArray,
                                val sshKeys: String,
                                override val upstreamGroups: MutableList<MachineGroup>,
                                override val postmortems: MutableList<Postmortem>,
                                override val downStreamKonfigurator: DownStreamKonfigurator? = null,
                                override val upStreamKonfigurator: UpStreamKonfigurator? = null) : MachineGroup{
    override var disabled: Boolean = false
    override val min: Int = members.size
    override val max: Int = members.size
    override val hardMax: Int = members.size


    override val downStreamGroups: MutableList<MachineGroup> = ArrayList()
    override var enabled: Boolean = true
    override val machineMonitorRules: SortedSet<MonitorRule<MachineState, Machine>> = TreeSet();
    override val groupMonitorRules: SortedSet<MonitorRule<MachineGroupState, MachineGroup>> = TreeSet()

    override val stateMachine = DefaultStateMachine<MachineGroupState>(this);
    override val monitor: Monitor<MachineGroupState, MachineGroup> = StaticMachineGroupMonitor(this, sensors, controller)
    override val defaultMachineRules = DefaultStateMachineRules<MachineState>();

    val machines = ConcurrentHashMap<String, StaticMachine>();

    {
        members.forEach { machines.put(it.name(), it) }
        machines().forEach { it.fsm.rules = defaultMachineRules }
        stateMachine.rules = DefaultStateMachineRules<MachineGroupState>();
        upstreamGroups.forEach { (it).downStreamGroups.add(this) }

    }

    override fun costPerHourInDollars(): Double = machines.map { it.getValue().costPerHourInDollars() }.sum()
    override fun groupName(): String = name;
    override fun name(): String = name;

    override fun machines(): List<Machine> {
        val arrayList: ArrayList<StaticMachine> = ArrayList();
        synchronized(machines) {
            arrayList.addAll(machines.values())
        }
        return arrayList;
    }

    override fun contract(): MachineGroup {
        println("CONTRACTING GROUP ${name()} REQUESTED BUT GROUP IS STATIC")
        return this
    }


    override fun expand(): Machine {
        println("EXPANDING GROUP ${name()} REQUESTED BUT GROUP IS STATIC")
        throw  UnsupportedOperationException();
    }


    override fun destroy(machine: Machine): MachineGroup {
        println("DESTROY $machine requested but group is static so rebuilding")
        rebuild(machine);
        return this;
    }


    override fun rebuild(machine: Machine): MachineGroup {
        super.rebuild(machine)
        val id = machine.id();
        try {
            println("Rebuilding $machine")
            rebuildAction.onHost(machine.ip(), user)
            Thread.sleep(300 * 1000)
            println("Rebuilt ${id}")
        } catch (e: Exception) {
            println("Failed to rebuild ${id} due to ${e.getMessage()}")
            machine.fsm.attemptTransition(MachineState.DEAD)
        }
        return this;
    }


    override fun restart(machine: Machine): MachineGroup {
        val id = machine.id();
        try {
            println("Restarting $machine")
            restartAction.onHost(machine.ip(), user)
            Thread.sleep(300 * 1000)
            println("Restarted ${id}")
        } catch (e: Exception) {
            println("Failed to restart ${id} due to ${e.getMessage()}")
            machine.fsm.attemptTransition(MachineState.DEAD)
        }

        return this;
    }
}