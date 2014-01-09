package kontrol.api

import kontrol.api.sensors.SensorArray
import java.util.ArrayList


/**
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 * @todo document.
 */
public trait MachineGroup : HasStateMachine<MachineGroupState, MachineGroup> {


    override val stateMachine: StateMachine<MachineGroupState, MachineGroup>
    val sensorArray: SensorArray<Any?>;
    val defaultMachineRules: StateMachineRules<MachineState, Machine>
    val monitor: Monitor<MachineGroupState, MachineGroup>
    val machineMonitorRules: MutableList<MonitorRule<MachineState, Machine>>
    val groupMonitorRules: MutableList<MonitorRule<MachineGroupState, MachineGroup>>
    val minSize: Int
    val maxSize: Int

    override fun name(): String

    fun size(): Int {
        return machines().filter { it.state() == MachineState.MACHINE_OK }.size();
    }

    fun machines(): List<Machine>

    fun startMonitoring() {
        println("Started monitoring ${name()}")
        machines().forEach { it.startMonitoring(machineMonitorRules) }
        monitor.start(this, stateMachine, groupMonitorRules);
    }
    fun stopMonitoring() {
        monitor.stop();
        machines().forEach { it.stopMonitoring() }
    }

    fun other(machine: Machine): Machine {
        return machines().filter { it != machine } [0];
    }

    fun failover(machine: Machine): MachineGroup {
        println("**** Failover Machine ${machine.ip()}");
        return this;
    }
    fun failback(machine: Machine): MachineGroup {
        println("**** Failback Machine ${machine.ip()}");
        return this;
    }
    fun configure(): MachineGroup {
        println("**** Configure ${name()}");
        return this;
    }
    fun expand(): MachineGroup {
        println("**** Expand ${name()}");
        return this;
    }
    fun contract(): MachineGroup {
        println("**** Contract ${name()}");
        return this;
    }
    fun reImage(machine: Machine): MachineGroup {
        println("**** Re Image Machine ${machine.ip()}");
        return this;
    }
    fun restart(machine: Machine): MachineGroup {
        println("**** Re Start Machine ${machine.ip()}");
        return this;
    }
    fun destroy(machine: Machine): MachineGroup {
        println("**** Destroy Machine ${machine.ip()}");
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
        return string;
    }

    public enum class Recheck {
        THEN
    }

    public class RuleBuilder1(val machineGroup: MachineGroup,
                              val newState: MachineState?) {


        var registry: Controller? = null;
        var recheck = false;

        fun RECHECK(b: Recheck): RuleBuilder1 {
            recheck = b == Recheck.THEN;
            return this;
        }


        fun TELL(registry: Controller): RuleBuilder1 {
            this.registry = registry;
            return this;
        }

        fun TO(action: Action): MachineGroup {
            machineGroup.defaultMachineRules.on(null, newState, {
                if (!recheck || it.stateMachine.state() == newState) {
                    registry?.execute(machineGroup, it, action)
                }
            });
            return machineGroup;
        }

    }

    class RuleBuilder2(val machineGroup: MachineGroup,
                       val newState: MachineGroupState) {
        public val THEN: Boolean = true;


        var registry: Controller? = null;
        var recheck = false;

        fun RECHECK(b: Recheck): RuleBuilder2 {
            recheck = b == Recheck.THEN;
            return this;
        }


        fun USE(registry: Controller): RuleBuilder2 {
            this.registry = registry;
            return this;
        }

        fun TO(action: GroupAction): MachineGroup {
            machineGroup.stateMachine.rules?.on(null, newState, {
                if (!recheck || it.stateMachine.state() == newState) {
                    registry?.execute(machineGroup, action)
                }
            });
            return machineGroup;
        }

    }

    class MachineStateRuleBuilder(val machineGroup: MachineGroup,
                                  val state: MachineState) {
        var condition: (Machine) -> Boolean = { true };
        var confirms = 0;
        var previousStates = ArrayList<MachineState>()

        fun WHEN(condition: (Machine) -> Boolean): MachineStateRuleBuilder {
            this.condition = condition;
            return this;
        }

        fun EVERY(confirms: Int): MachineStateRuleBuilder {
            this.confirms = confirms;
            return this;
        }
        fun FROM(states: List<MachineState>): MachineStateRuleBuilder {
            previousStates.addAll(states)
            return this;
        }

        fun CALL_THIS(name: String) {
            machineGroup.machineMonitorRules.add(MonitorRule(state, condition, confirms, name, previousStates))
            println("Added rule for $name")
        }


    }

    class MachineGroupStateRuleBuilder(val machineGroup: MachineGroup,
                                       val state: MachineGroupState) {
        var condition: (MachineGroup) -> Boolean = { true };
        var confirms = 0;
        var previousStates = ArrayList<MachineGroupState?>()

        fun WHEN(condition: (MachineGroup) -> Boolean): MachineGroupStateRuleBuilder {
            this.condition = condition;
            return this;
        }

        fun EVERY(confirms: Int): MachineGroupStateRuleBuilder {
            this.confirms = confirms;
            return this;
        }
        fun FROM(states: List<MachineGroupState?>): MachineGroupStateRuleBuilder {
            previousStates.addAll(states)
            return this;
        }

        fun CALL_THIS(name: String) {
            machineGroup.groupMonitorRules.add(MonitorRule(state, condition, confirms, name, previousStates))
            println("Added rule for $name")
        }


    }

    fun HAVE(newState: MachineState): RuleBuilder1 {
        return  RuleBuilder1(this, newState);
    }
    fun ARE(newState: MachineGroupState): RuleBuilder2 {
        return  RuleBuilder2(this, newState);
    }
    fun MACHINE_STATE(state: MachineState): MachineStateRuleBuilder {
        return  MachineStateRuleBuilder(this, state);
    }
    fun STATE(state: MachineGroupState): MachineGroupStateRuleBuilder {
        return  MachineGroupStateRuleBuilder(this, state);
    }

}
