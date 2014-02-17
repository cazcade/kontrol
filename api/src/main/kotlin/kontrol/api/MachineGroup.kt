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

import kontrol.api.sensors.SensorArray
import java.util.SortedSet
import kontrol.ext.collections.avgAsDouble
import kontrol.ext.collections.median


/**
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 * @todo document.
 */
public trait MachineGroup : Monitorable<MachineGroupState> {

    val controller: Controller
    val postmortems: List<Postmortem>
    val upStreamKonfigurator: UpStreamKonfigurator?
    val downStreamKonfigurator: DownStreamKonfigurator?
    val stateMachine: StateMachine<MachineGroupState>
    val sensors: SensorArray;
    val defaultMachineRules: StateMachineRules<MachineState>
    val monitor: Monitor<MachineGroupState, MachineGroup>
    val machineMonitorRules: SortedSet<MonitorRule<MachineState, Machine>>
    val groupMonitorRules: SortedSet<MonitorRule<MachineGroupState, MachineGroup>>
    val upstreamGroups: MutableList<MachineGroup>
    val downStreamGroups: MutableList<MachineGroup>
    val min: Int
    val max: Int
    val hardMax: Int


    override fun id(): String = name()

    override fun state(): MachineGroupState? {
        return stateMachine.state()
    }
    override fun transition(state: MachineGroupState?) {
        stateMachine.transition(state)
    }
    override fun name(): String

    fun machines(): List<Machine>

    fun workingSize(): Int = workingMachines().size();
    fun activeSize(): Int = enabledMachines().filter { !(it.state() in listOf(MachineState.FAILED, MachineState.REPAIR, MachineState.DEAD, MachineState.BROKEN, MachineState.UPGRADE_FAILED)) }.size();
    fun enabledMachines(): List<Machine> = machines().filter { it.enabled }
    fun brokenMachines(): List<Machine> = machines().filter { it.enabled && it.state() in listOf(MachineState.FAILED, MachineState.DEAD, MachineState.BROKEN, MachineState.UPGRADE_FAILED) }
    fun workingMachines(): List<Machine> = enabledMachines().filter { it.state() in listOf(MachineState.OK, MachineState.STALE, null) }
    fun workingAndReadyMachines(): List<Machine> = enabledMachines().filter { it.state() in listOf(MachineState.OK, MachineState.STALE) }

    fun get(value: String): Double? {
        val values = machines()  map { it[value] }
        val average = values.map { it?.D() }.avgAsDouble()
        val median = values.map { it?.D() }.median()
        //The average should be within a factor of 5 of the median, if not we use the median instead
        val result = when {
            average == null -> null
            median == null -> null
            average!! > median!! * 5 -> {
                median
            }
            average!! < median!! / 5 -> {
                median
            }
            else -> {
                average
            }
        }
        //        println("$value was $result")
        return result;
    }


    fun clearState(machine: Machine) {
        machine.transition(null);

    }
    fun postmortem(machine: Machine): List<PostmortemResult> = postmortems.map {
        try {
            it.perform(machine)
        } catch(e: Exception) {
            e.printStackTrace(); null
        }
    }.filterNotNull()

    fun startMonitoring() {
        println("Started monitoring ${name()}")
        machines().forEach { it.startMonitoring(machineMonitorRules) }
        monitor.start(this, stateMachine, groupMonitorRules);
    }
    fun stopMonitoring() {
        monitor.stop();
        machines().forEach { it.stopMonitoring() }
    }

    fun other(machine: Machine): Machine? {
        val list = workingAndReadyMachines().filter { it != machine }
        return if (list.size() > 0) {
            list.first()
        } else {
            null
        }
    }


    fun failAction(machine: Machine, action: (Machine) -> Unit) {
        println("**** Fail Action for  Machine ${machine.ip()}");
        try {
            failover(machine);
            action(machine);
        } catch(e: Exception) {
            e.printStackTrace();
        }
    }

    fun failover(machine: Machine): MachineGroup {
        if (other(machine) == null) {
            throw IllegalStateException("No machine to take over cannot failover")
        } else {
            println("**** Failover Machine ${machine.ip()}");
            try {
                downStreamKonfigurator?.onMachineFail(machine, this);
                upStreamKonfigurator?.onMachineFail(machine, this)
                upstreamGroups.forEach { it.downstreamFailover(machine, this) }
            } catch (e: Exception) {
                throw e
            }
            return this;
        }
    }

    fun failback(machine: Machine): MachineGroup {
        println("**** Failback Machine ${machine.ip()}");
        machine.enable()
        downStreamKonfigurator?.onMachineUnfail(machine, this);
        upStreamKonfigurator?.onMachineUnfail(machine, this)
        upstreamGroups.forEach { it.downstreamFailback(machine, this) }
        return this;
    }

    fun downstreamFailover(machine: Machine,
                           machineGroup: MachineGroup) {
        println("**** Downstream Failover Machine ${machine.ip()} for Group ${machineGroup.name()}");
        downStreamKonfigurator?.onDownStreamMachineFail(machine, machineGroup, this)
        downStreamKonfigurator?.configureDownStream(this)
    }

    fun downstreamFailback(machine: Machine,
                           machineGroup: MachineGroup) {
        println("**** Downstream Failback Machine ${machine.ip()} for Group ${machineGroup.name()}");
        downStreamKonfigurator?.onDownStreamMachineUnfail(machine, machineGroup, this)
        downStreamKonfigurator?.configureDownStream(this)
    }

    fun costPerHourInDollars(): Double

    fun costPerMonthInDollars(): Double = costPerHourInDollars() * 24 * 30

    fun configure(): MachineGroup {
        println("**** Configure ${name()}");
        upStreamKonfigurator?.configureUpStream(this)
        downStreamKonfigurator?.configureDownStream(this)
        return this;
    }

    fun configure(machine: Machine): Machine {
        println("**** Configure ${name()}");
        upStreamKonfigurator?.configureUpStream(this)
        downStreamKonfigurator?.configureDownStream(this, machine)
        return machine;
    }


    fun expand(): Machine {
        println("**** Expand ${name()}");
        throw UnsupportedOperationException()
    }
    fun contract(): MachineGroup {
        println("**** Contract ${name()}");
        return this;
    }
    fun rebuild(machine: Machine): MachineGroup {
        println("**** Re Image Machine ${machine.name()}(${machine.id()})");
        return this;
    }
    fun fix(machine: Machine): MachineGroup {
        println("**** Re Start Machine ${machine.name()}(${machine.id()})");
        return this;
    }
    fun destroy(machine: Machine): MachineGroup {
        println("**** Destroy Machine ${machine.name()}(${machine.id()})");
        return this;
    }


    fun on(oldState: MachineState? = null, newState: MachineState, action: ((Machine) -> Unit)? = null): MachineGroup {
        defaultMachineRules.on(oldState, newState, action)
        return this
    }


    fun allow(oldState: MachineState, newState: MachineState): MachineGroup {
        defaultMachineRules.allow(oldState, newState)
        return this
    }

    fun allowMachine(pair: Pair<MachineState, MachineState>): MachineGroup {
        defaultMachineRules.allow(pair.first, pair.second)
        return this
    }


    fun allow(pair: Pair<MachineGroupState, MachineGroupState>): MachineGroup {
        stateMachine.rules?.allow(pair.first, pair.second)
        return this
    }

    fun onGroup(oldState: MachineGroupState? = null, newState: MachineGroupState, action: (MachineGroup) -> Unit): MachineGroup {
        stateMachine.rules?.on(oldState, newState, action)
        return this
    }


    fun toString(): String {
        var string: String = "${name()} [${stateMachine.state()}]\n";
        for (machine in machines()) {
            string += "$machine\n";
        }
        string += "Rules:"
        machineMonitorRules.forEach { string += "$it\n" }
        return string;
    }

    public enum class Recheck {
        THEN
    }

    public class RuleBuilder1(val machineGroup: MachineGroup,
                              val newState: MachineState?) {


        var controller: Controller? = null;
        var recheck = false;

        fun recheck(b: Recheck): RuleBuilder1 {
            recheck = b == Recheck.THEN;
            return this;
        }


        fun tell(registry: Controller): RuleBuilder1 {
            this.controller = registry;
            return this;
        }

        fun takeAction(vararg actions: Action): MachineGroup {
            machineGroup.defaultMachineRules.on<Machine>(null, newState, { machine ->
                if (!recheck || machine.fsm.state() == newState) {
                    actions.forEach { action -> println("**** TAKING ACTION $action ****");controller?.execute(machineGroup, machine, { true }, action) }
                } else {
                    println("RECHECK FAILED for $actions")
                }
            });
            return machineGroup;
        }

        fun takeActions(actions: List<Action>): MachineGroup {
            machineGroup.defaultMachineRules.on<Machine>(null, newState, { machine ->
                if (!recheck || machine.fsm.state() == newState) {
                    actions.forEach { action ->
                        println("**** TAKING ACTION $action ON ${machine.id()} ****");
                        controller?.execute(machineGroup, machine, { true }, action)
                    }
                } else {
                    println("RECHECK FAILED for $actions")
                }
            });
            return machineGroup;
        }

    }

    class RuleBuilder2(val machineGroup: MachineGroup,
                       val newState: MachineGroupState) {
        public val yes: Boolean = true;


        var registry: Controller? = null;
        var recheck = false;

        fun recheck(b: Recheck): RuleBuilder2 {
            recheck = b == Recheck.THEN;
            return this;
        }


        fun use(registry: Controller): RuleBuilder2 {
            this.registry = registry;
            return this;
        }

        fun takeActions(actions: List<GroupAction>): MachineGroup {
            actions.forEach { takeAction(it) }
            return machineGroup;
        }

        fun takeAction(action: GroupAction): MachineGroup {
            machineGroup.stateMachine.rules?.on<MachineGroup>(null, newState, {
                if (!recheck || machineGroup.stateMachine.state() == newState) {
                    registry?.execute(machineGroup, { true }, action)
                } else {
                    println("RECHECK FAILED for $action")
                }
            });
            return machineGroup;
        }

    }

    class MachineStateRuleBuilder(val machineGroup: MachineGroup,
                                  val state: MachineState) {
        var condition: (Machine) -> Boolean = { true };
        var confirms = 0;
        var previousStates = hashSetOf<MachineState?>()

        fun andTest(condition: (Machine) -> Boolean): MachineStateRuleBuilder {
            this.condition = condition;
            return this;
        }

        fun after(seconds: Int): MachineStateRuleBuilder {
            this.confirms = seconds / machineGroup.controller.frequency;
            return this;
        }
        fun ifStateIn(states: List<MachineState?>): MachineStateRuleBuilder {
            previousStates.addAll(states)
            return this;
        }

        fun seconds(name: String) {
            machineGroup.machineMonitorRules.add(MonitorRule(state, condition, confirms, name, previousStates))
            println("Added rule for $name")
        }


    }

    class MachineGroupStateRuleBuilder(val machineGroup: MachineGroup,
                                       val state: MachineGroupState) {
        var condition: (MachineGroup) -> Boolean = { true };
        var confirms = 0;
        var previousStates = hashSetOf<MachineGroupState?>()

        fun andTest(condition: (MachineGroup) -> Boolean): MachineGroupStateRuleBuilder {
            this.condition = condition;
            return this;
        }

        fun after(seconds: Int): MachineGroupStateRuleBuilder {
            this.confirms = seconds / machineGroup.controller.frequency;
            return this;
        }
        fun ifStateIn(states: List<MachineGroupState?>): MachineGroupStateRuleBuilder {
            previousStates.addAll(states)
            return this;
        }

        fun seconds(name: String) {
            machineGroup.groupMonitorRules.add(MonitorRule(state, condition, confirms, name, previousStates))
            println("Added rule for $name")
        }


    }

    fun whenMachine(newState: MachineState): RuleBuilder1 {
        return  RuleBuilder1(this, newState);
    }
    fun whenGroup(newState: MachineGroupState): RuleBuilder2 {
        return  RuleBuilder2(this, newState);
    }
    fun memberIs(state: MachineState): MachineStateRuleBuilder {
        return  MachineStateRuleBuilder(this, state);
    }
    fun becomes(state: MachineGroupState): MachineGroupStateRuleBuilder {
        return  MachineGroupStateRuleBuilder(this, state);
    }

}

/*

fun List<SensorValue?>.sumSensors(): Double? {
    return if (this.size() > 0) this.map { if (it?.v != null) it?.D() else null } .reduce { x, y ->
        when {
            x == null -> y
            y == null -> x
            else -> x + y
        }
    } else null
}

fun List<SensorValue?>.median(): Double {
    val sorted = this
            .map { if (it?.v != null) it?.D() else null }
            .filterNotNull()
            .sortBy { it }
    if (sorted.size() > 0) {
        return sorted.get(sorted.size() / 2)
    } else {
        return 0.0;
    }
}


fun List<SensorValue?>.avg(): Double? {
    val sum = this.sumSensors()
    val size = this.filterNotNull().size
    return when {
        sum == null -> null
        size == 0 -> 0.0
        else -> sum / size
    };
}


fun List<SensorValue?>.max(): Double? {
    return if (this.size() > 0) this.map { if (it?.v != null) it?.v.toString().toDouble() else null } .reduce { x, y ->
        when {
            x == null -> y
            y == null -> x
            x!! > y!! -> x
            else -> y
        }
    } else null
}
fun List<SensorValue?>.min(): Double? {
    return if (this.size() > 0) this.map { if (it?.v != null) it?.v.toString().toDouble() else null } .reduce { x, y ->
        when {
            x == null -> y
            y == null -> x
            x!! < y!! -> x
            else -> y
        }
    } else null
}

*/