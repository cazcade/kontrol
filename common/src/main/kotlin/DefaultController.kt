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
import java.util.concurrent.CopyOnWriteArraySet

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public class DefaultController(val bus: Bus, val eventLog: EventLog, val timeoutInMinutes: Long = 30) : Controller{

    override final val frequency: Int = 15
    var gracePeriod: Int = 0
    var started: Long = 0

    val  groupMonitors: ConcurrentHashMap<Monitor<MachineGroupState, MachineGroup>, Pair<MachineGroup, Set<MonitorRule<MachineGroupState, MachineGroup>>>> = ConcurrentHashMap()
    val  machineMonitors: ConcurrentHashMap<Monitor<MachineState, Machine>, Pair<Machine, Set<MonitorRule<MachineState, Machine>>>> = ConcurrentHashMap()
    val actions = HashMap<String, Pair<() -> Boolean, (Machine) -> Serializable>>();
    val groupActions = HashMap<String, Pair<() -> Boolean, (MachineGroup) -> Serializable>>();
    var executor: ScheduledExecutorService? = null
    val monitorExec: FountainExecutorService = FountainExecutorServiceImpl("Machine Monitor Queue", 0, 10, 98, 100, 20);
    val groupMonitorExec: FountainExecutorService = FountainExecutorServiceImpl("Group Monitor Queue", 0, 10, 97, 100, 20);
    var groupExec: FountainExecutorService = FountainExecutorServiceImpl("Group Exec Queue", 0, 256, 100, 100, 1);
    var machineExec: FountainExecutorService = FountainExecutorServiceImpl("Machine Exec Queue", 0, 256, 100, 100, 1);
    val unExecutedActions: MutableSet<String> = CopyOnWriteArraySet()

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
        groupMonitorExec.start();
        monitorExec.start();
        machineExec.start();
        executor?.scheduleWithFixedDelay({
            //            println("*** Group Heartbeat ${Date()} ***")
            try {

                groupMonitors.keySet().forEach {
                    if (it.target()?.enabled?:false) {
                        val details = groupMonitors.get(it);
                        groupMonitorExec.submit(false, details?.first?.id()) {
                            if (details != null) {
                                details.second.forEach { it.evaluate(details.first, eventLog) }
                            }
                            it.heartbeat()
                        }
                    } else {
                        println("Group monitor rules skipped as group ${it.target()?.id()} is disabled");
                    }

                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 0, frequency.toLong(), TimeUnit.SECONDS)
        executor?.scheduleWithFixedDelay({
            try {

                //                println("*** Machine Heartbeat ${Date()} ***")
                machineMonitors.keySet().forEach {
                    if (it.target()?.enabled?:false) {
                        val details = machineMonitors.get(it);
                        monitorExec.submit(false, it.target()?.id()) {
                            if (details != null) {
                                details.second.forEach { it.evaluate(details.first, eventLog) }
                            }
                            it.heartbeat()
                        }
                    } else {
                        println("Machine monitor rules skipped as machine ${it.target()?.id()} is disabled");
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 0, frequency.toLong(), TimeUnit.SECONDS)

        executor?.scheduleWithFixedDelay({
            try {
                groupMonitors.keySet().forEach {
                    groupMonitorExec.execute(false, it.target()?.groupName()) {
                        it.update() ;
                        //println("*** Machine Group Update ${Date()} ***")
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }

        }, 1, 2, TimeUnit.MINUTES)
        executor?.scheduleWithFixedDelay({
            try {
                val monitors = HashMap(machineMonitors)
                monitors.keySet().forEach {
                    if (it.target()?.enabled?:false) {
                        monitorExec.execute(false, it.target()?.id()) {
                            it.update() ;
                            //println("*** Machine Update ${Date()} ***")
                        }
                    } else {
                        println("Machine sensor update skipped as machine ${it.target()?.id()} is disabled");
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }, 2, 30, TimeUnit.SECONDS)
    }

    override fun stop() {
        executor?.shutdown();
        executor = null
        machineExec.stop();
        groupExec.stop();
        groupMonitorExec.stop();
        monitorExec.stop();
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
        if (group.disabled) {
            println("Group ${group.name()} was disabled")
            return this

        }
        if (gracePeriod * 1000 + started > System.currentTimeMillis()) {
            println("Actions ignored during grace period.")
            return this
        }

        //        println(actions);
        actionArgs.forEach { actionArg ->
            val key = key(group, actionArg)
            val executionKey = key + ":" + machine.id();
            val action = actions[key];
            if (executionKey in unExecutedActions) {
                println("Skipping group action for $actionArg on ${machine.id()}")
            } else
                if (action != null && action.first() && key !in executionKey) {
                    println("Performing group action for $actionArg on ${machine.ip()}")
                    bus.dispatch("machine.action.pre", actionArg to machine.id());
                    unExecutedActions.add(executionKey);
                    machineExec.submit(false, machine.id()) {
                        machine.disable();
                        try {
                            unExecutedActions.remove(executionKey);
                            eventLog.log(machine.name(), actionArg, LogContextualState.START)
                            bus.dispatch("machine.action.post", actionArg to action.second(machine));
                            println("Performed action for $actionArg on ${machine.ip()}")
                        } finally {
                            machine.enable();
                            eventLog.log(machine.name(), actionArg, LogContextualState.END)
                        }
                    }
                };
            val key2 = key(machine, actionArg)
            val action2 = actions[key2];
            val executionKey2 = key2;
            if (executionKey2 in unExecutedActions) {
                println("Skipping action for $actionArg on ${machine.id()}")
            } else if (action2 != null && action2.first() ) {
                println("Performing action for $actionArg on ${machine.ip()}")
                bus.dispatch("machine.action.pre", actionArg to machine.id());
                unExecutedActions.add(executionKey2)
                machineExec.submit(false, machine.id()) {
                    machine.disable();
                    try {
                        unExecutedActions.remove(executionKey2);
                        //record result into event log
                        eventLog.log(machine.name(), actionArg, LogContextualState.START)
                        bus.dispatch("machine.action.post", actionArg to  action2.second(machine));
                        println("Performed action for $actionArg on ${machine.ip()}")
                    } finally {
                        machine.enable();
                        eventLog.log(machine.name(), actionArg, LogContextualState.END)
                    }
                }
            };
        }
        return this;
    }


    override fun execute(group: MachineGroup, pre: () -> Boolean, vararg actionArgs: GroupAction): Controller {
        if (group.disabled) {
            println("Group ${group.name()} was disabled")
            return this
        }

        if (gracePeriod * 1000 + started > System.currentTimeMillis()) {
            println("Actions ignored during grace period.")
            return this
        }
        actionArgs.forEach { actionArg ->
            val key = key(group, actionArg)

            println(key)
            val action = groupActions[key];
            if (key in unExecutedActions) {
                println("Skipping action for $actionArg on ${group.name()}")

            } else if (action != null && action.first()) {
                println("Performing action for $actionArg on ${group.name()}")
                bus.dispatch("machine.group.pre", actionArg to group.name());
                unExecutedActions.add(key);
                groupExec.submit(false, group.groupName()) {
                    group.disabled = true;
                    try {
                        unExecutedActions.remove(key)
                        eventLog.log(group.name(), actionArg, LogContextualState.START)
                        bus.dispatch("machine.group.post", actionArg to action.second(group));
                        println("Performed action for $actionArg on ${group.name()}")
                    } finally {
                        group.disabled = false;
                        eventLog.log(group.name(), actionArg, LogContextualState.END)
                    }
                }
            };
        }
        return this;
    }

    override fun execute(group: MachineGroup, pre: () -> Boolean, vararg actionArgs: Action): Controller {
        if (group.disabled) {
            println("Group ${group.name()} was disabled")
            return this
        }

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
                    val executionKey = key + ":" + it.id();
                    if (executionKey in unExecutedActions) {
                        println("Skipping action for $actionArg on ${it.id()}")
                    } else {
                        println("Performing action for $actionArg on ${it.id()}")
                        bus.dispatch("machine.group.pre", actionArg to group.name());
                        unExecutedActions.add(executionKey);
                        machineExec.submit(false, it.id()) {
                            it.disable();
                            try {
                                unExecutedActions.remove(executionKey);
                                eventLog.log(group.name(), actionArg, LogContextualState.START)
                                bus.dispatch("machine.group.post", actionArg to  action.second(it));
                            } finally {
                                it.enable();
                                eventLog.log(group.name(), actionArg, LogContextualState.END)
                            }
                        }
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