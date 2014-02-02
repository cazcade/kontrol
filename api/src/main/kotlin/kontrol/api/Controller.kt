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

import java.io.Serializable


/**
 *
 * The implementation specific part.
 *
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 * @todo document.
 */
public trait Controller {
    val frequency: Int
    fun execute(group: MachineGroup, machine: Machine, pre: () -> Boolean = { true }, vararg actionArgs: Action): Controller
    fun execute(group: MachineGroup, pre: () -> Boolean = { true }, vararg actionArgs: GroupAction): Controller
    fun execute(group: MachineGroup, pre: () -> Boolean = { true }, vararg actionArgs: Action): Controller

    fun monitor(machine: Machine, alerter: Alerter, vararg states: MachineState): Controller
    fun monitor(machineGroup: MachineGroup, alerter: Alerter, vararg states: MachineGroupState): Controller

    fun register(group: MachineGroup, action: Action, pre: () -> Boolean = { true }, machineAction: (Machine) -> Serializable): Controller
    fun register(machine: Machine, action: Action, pre: () -> Boolean = { true }, machineAction: (Machine) -> Serializable): Controller
    fun register(group: MachineGroup, action: GroupAction, pre: () -> Boolean = { true }, machineGroupAction: (MachineGroup) -> Serializable): Controller


    fun start(gracePeriod: Int)
    fun stop()

    fun addGroupMonitor(monitor: Monitor<MachineGroupState, MachineGroup>,
                        target: MachineGroup,
                        rules: Set<MonitorRule<MachineGroupState, MachineGroup>>)

    fun removeGroupMonitor(monitor: Monitor<MachineGroupState, MachineGroup>)


    fun addMachineMonitor(monitor: Monitor<MachineState, Machine>,
                          target: Machine,
                          rules: Set<MonitorRule<MachineState, Machine>>)

    fun removeMachineMonitor(monitor: Monitor<MachineState, Machine>)


    class MachineRuleBuilder(val actionRegistry: Controller,
                             val action: (Machine) -> Serializable){
        var machineAction: Action? = null;
        var ifClause: () -> Boolean = { true };

        fun IF(unless: () -> Boolean): MachineRuleBuilder {
            this.ifClause = unless;
            return this;
        }


        fun takeAction(machineAction: Action): MachineRuleBuilder {
            this.machineAction = machineAction;
            return this;
        }

        fun inGroup(group: MachineGroup) {
            if (machineAction != null) {
                actionRegistry.register(group, machineAction!!, ifClause, {
                    if (ifClause()) {
                        action(it)
                    } else {
                        java.lang.String()
                    }
                });
            }

        }
    }


    //todo: generify if possible
    class MachineGroupRuleBuilder(val actionRegistry: Controller,
                                  val action: (MachineGroup) -> Serializable){
        var groupAction: GroupAction? = null;
        var ifClause: () -> Boolean = { true };

        fun to(groupAction: GroupAction): MachineGroupRuleBuilder {
            this.groupAction = groupAction;
            return this;
        }

        fun IF(ifClause: () -> Boolean): MachineGroupRuleBuilder {
            this.ifClause = ifClause;
            return this;
        }

        fun group(group: MachineGroup) {
            if (groupAction != null) {
                actionRegistry.register(group, groupAction!!, ifClause, { action(it) });
            }

        }
    }

    fun will(action: (Machine) -> Serializable): MachineRuleBuilder {
        return MachineRuleBuilder(this, action);
    }
    fun use(action: (MachineGroup) -> Serializable): MachineGroupRuleBuilder {
        return MachineGroupRuleBuilder(this, action);
    }
}
