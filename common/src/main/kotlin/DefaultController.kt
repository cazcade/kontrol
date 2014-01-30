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

package kontrol.common


import kontrol.api.Controller
import kontrol.api.Machine
import kontrol.api.Action
import kontrol.api.MachineGroup
import kontrol.api.GroupAction
import kontrol.api.Alerter
import kontrol.api.MachineState
import kontrol.api.MachineGroupState
import kontrol.api.Bus
import java.io.Serializable
import kontrol.api.Monitor
import kontrol.api.MonitorRule
import java.util.concurrent.TimeUnit
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.Executors
import exec.FountainExecutorService
import exec.FountainExecutorServiceImpl
import kontrol.api.EventLog
import kontrol.api.LogContextualState

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public class DefaultController(val bus: Bus, val eventLog: EventLog, val timeoutInMinutes: Long = 30) : Controller{

    override val frequency: Int = 15
    var gracePeriod: Int = 0
    var started: Long = 0

    val  groupMonitors: ConcurrentHashMap<Monitor<MachineGroupState, MachineGroup>, Pair<MachineGroup, Set<MonitorRule<MachineGroupState, MachineGroup>>>> = ConcurrentHashMap()
    val  machineMonitors: ConcurrentHashMap<Monitor<MachineState, Machine>, Pair<Machine, Set<MonitorRule<MachineState, Machine>>>> = ConcurrentHashMap()
    val actions = HashMap<String, Pair<() -> Boolean, (Machine) -> Serializable>>();
    val groupActions = HashMap<String, Pair<() -> Boolean, (MachineGroup) -> Serializable>>();
    var executor: ScheduledExecutorService? = null
    var groupExec: FountainExecutorService = FountainExecutorServiceImpl(0, 32, 10, 100, 1);

    override fun addGroupMonitor(monitor: Monitor<MachineGroupState, MachineGroup>, target: MachineGroup, rules: Set<MonitorRule<MachineGroupState, MachineGroup>>) {
        groupMonitors.put(monitor, target to rules)
    }
    override fun removeGroupMonitor(monitor: Monitor<MachineGroupState, MachineGroup>) {
        groupMonitors.remove(monitor)
    }
    override fun addMachineMonitor(monitor: Monitor<MachineState, Machine>, target: Machine, rules: Set<MonitorRule<MachineState, Machine>>) {
        machineMonitors.put(monitor, target to rules)
    }
    override fun removeMachineMonitor(monitor: Monitor<MachineState, Machine>) {
        machineMonitors.remove(monitor)
    }
    override fun start(gracePeriod: Int) {
        this.gracePeriod = gracePeriod
        this.started = System.currentTimeMillis()
        executor = Executors.newSingleThreadScheduledExecutor()
        groupExec.start();
        executor?.scheduleWithFixedDelay({
            groupMonitors.keySet().forEach {
                val details = groupMonitors.get(it);
                groupExec.submit(false, details?.first?.id()) {
                    if (details != null) {
                        details.second.forEach { it.evaluate(details.first, eventLog) }
                    }
                    it.heartbeat()
                }
            }
        }, 0, frequency.toLong(), TimeUnit.SECONDS)
        executor?.scheduleWithFixedDelay({
            machineMonitors.keySet().forEach {
                val details = machineMonitors.get(it);
                groupExec.submit(false, details?.first?.id()) {
                    if (details != null) {
                        details.second.forEach { it.evaluate(details.first, eventLog) }
                    }
                    it.heartbeat()
                }
            }
            println("*** Heartbeat ${Date()} ***")
        }, 0, frequency.toLong(), TimeUnit.SECONDS)

        executor?.scheduleWithFixedDelay({
            groupMonitors.keySet().forEach {
                groupExec.submit(false, it.target()?.id() + ".monitor") { it.update() }
            }
            println("*** Machine Group Update ${Date()} ***")
        }, 0, 5, TimeUnit.MINUTES)

        executor?.scheduleWithFixedDelay({
            val monitors = HashMap(machineMonitors)
            monitors.keySet().forEach { groupExec.submit(false, it.target()?.id() + ".monitor") { it.update() } }
            println("*** Machine Update ${Date()} ***")
        }, 2, 30, TimeUnit.SECONDS)
    }

    override fun stop() {
        executor?.shutdown();
        executor = null
        groupExec.stop();
    }


    fun key(group: MachineGroup, action: Action): String {
        return "group.${group.name()}.action.${action}";
    }
    fun key(group: MachineGroup, action: GroupAction): String {
        return "group.${group.name()}.group-action.${action}";
    }

    fun key(machine: Machine, action: Action): String {
        return "machine.${machine.ip()}.action.${action}";
    }

    override fun execute(group: MachineGroup, machine: Machine, pre: () -> Boolean, vararg actionArgs: Action): Controller {
        if (gracePeriod * 1000 + started > System.currentTimeMillis()) {
            println("Actions ignored during grace period.")
            return this
        }

        //        println(actions);
        actionArgs.forEach { actionArg ->
            val action = actions[key(machine, actionArg)];
            if (action != null && action.first()) {
                println("Performing action for $actionArg on ${machine.ip()}")
                bus.dispatch("machine.action.pre", actionArg to machine.id());
                groupExec.submit(false, machine.id()) {
                    eventLog.log(machine.name(), actionArg, LogContextualState.START)
                    bus.dispatch("machine.action.post", actionArg to action.second(machine));
                    eventLog.log(machine.name(), actionArg, LogContextualState.END)
                }
            };
            val action2 = actions[key(group, actionArg)];
            if (action2 != null && action2.first()) {
                println("Performing action for $actionArg on ${machine.ip()}")
                bus.dispatch("machine.action.pre", actionArg to machine.id());
                groupExec.submit(false, machine.id()) {
                    eventLog.log(machine.name(), actionArg, LogContextualState.START)
                    bus.dispatch("machine.action.post", actionArg to  action2.second(machine));
                    eventLog.log(machine.name(), actionArg, LogContextualState.END)
                }
            };
        }
        return this;
    }


    override fun execute(group: MachineGroup, pre: () -> Boolean, vararg actionArgs: GroupAction): Controller {
        if (gracePeriod * 1000 + started > System.currentTimeMillis()) {
            println("Actions ignored during grace period.")
            return this
        }
        actionArgs.forEach { actionArg ->
            val key = key(group, actionArg)
            println(key)
            val action = groupActions[key];
            if (action != null && action.first()) {
                println("Performing action for $actionArg on ${actionArg.name()}")
                bus.dispatch("machine.group.pre", actionArg to group.name());
                groupExec.submit(false, group.id()) {
                    eventLog.log(group.name(), actionArg, LogContextualState.START)
                    bus.dispatch("machine.group.post", actionArg to action.second(group));
                    eventLog.log(group.name(), actionArg, LogContextualState.END)
                }
            };
        }
        return this;
    }

    override fun execute(group: MachineGroup, pre: () -> Boolean, vararg actionArgs: Action): Controller {
        if (gracePeriod * 1000 + started > System.currentTimeMillis()) {
            println("Actions ignored during grace period.")
            return this
        }

        println(actions);
        actionArgs.forEach { actionArg ->
            val key = key(group, actionArg)
            println(key)
            val action = actions[key];
            if (action != null && action.first()) {
                println("Action is not null")
                group.machines().forEach {
                    println("Performing action for $actionArg on ${it.ip()}")
                    bus.dispatch("machine.group.pre", actionArg to group.name());
                    groupExec.submit(false, it.id()) {
                    eventLog.log(group.name(), actionArg, LogContextualState.START)
                        bus.dispatch("machine.group.post", actionArg to  action.second(it));
                        eventLog.log(group.name(), actionArg, LogContextualState.END)
                    }
                };
            }
        }
        return this;
    }

    override fun monitor(machine: Machine, alerter: Alerter, vararg states: MachineState): Controller {
        throw UnsupportedOperationException()
    }
    override fun monitor(machineGroup: MachineGroup, alerter: Alerter, vararg states: MachineGroupState): Controller {
        throw UnsupportedOperationException()
    }
    override fun register(group: MachineGroup, action: Action, pre: () -> Boolean, machineAction: (Machine) -> Serializable): Controller {
        actions.put(key(group, action), Pair<() -> Boolean, (Machine) -> Serializable>(pre, machineAction));
        return this;
    }
    override fun register(machine: Machine, action: Action, pre: () -> Boolean, machineAction: (Machine) -> Serializable): Controller {
        actions.put(key(machine, action), Pair<() -> Boolean, (Machine) -> Serializable>(pre, machineAction));
        return this;
    }
    override fun register(group: MachineGroup, action: GroupAction, pre: () -> Boolean, machineGroupAction: (MachineGroup) -> Serializable): Controller {
        groupActions.put(key(group, action), Pair<() -> Boolean, (MachineGroup) -> Serializable>(pre, machineGroupAction));
        return this;
    }


}