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


/**
 *
 * The implementation specific part.
 *
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 * @todo document.
 */
public trait Controller {
    fun execute(group: MachineGroup, machine: Machine, vararg actionArgs: Action): Controller
    fun execute(group: MachineGroup, vararg actionArgs: GroupAction): Controller
    fun execute(group: MachineGroup, vararg actionArgs: Action): Controller

    fun monitor(machine: Machine, alerter: Alerter, vararg states: MachineState): Controller
    fun monitor(machineGroup: MachineGroup, alerter: Alerter, vararg states: MachineGroupState): Controller

    fun register(group: MachineGroup, action: Action, machineAction: (Machine) -> Unit): Controller
    fun register(machine: Machine, action: Action, machineAction: (Machine) -> Unit): Controller
    fun register(group: MachineGroup, action: GroupAction, machineGroupAction: (MachineGroup) -> Unit): Controller

    class MachineRuleBuilder(val actionRegistry: Controller,
                             val action: (Machine) -> Unit){
        var machineAction: Action? = null;
        var unless: () -> Boolean = { false };

        fun UNLESS(unless: () -> Boolean): MachineRuleBuilder {
            this.unless = unless;
            return this;
        }


        fun to(machineAction: Action): MachineRuleBuilder {
            this.machineAction = machineAction;
            return this;
        }

        fun inGroup(group: MachineGroup) {
            if (machineAction != null) {
                actionRegistry.register(group, machineAction!!, {
                    if (!unless()) action(it) else {
                        println("Precondition stopped action ${machineAction}")
                    }
                });
            }

        }
    }


    //todo: generify if possible
    class MachineGroupRuleBuilder(val actionRegistry: Controller,
                                  val action: (MachineGroup) -> Unit){
        var groupAction: GroupAction? = null;
        var unless: () -> Boolean = { false };

        fun to(groupAction: GroupAction): MachineGroupRuleBuilder {
            this.groupAction = groupAction;
            return this;
        }

        fun unless(unless: () -> Boolean): MachineGroupRuleBuilder {
            this.unless = unless;
            return this;
        }

        fun group(group: MachineGroup) {
            if (groupAction != null) {
                actionRegistry.register(group, groupAction!!, { if (!unless()) action(it) else println("Precondition stopped action ${groupAction}") });
            }

        }
    }

    fun will(action: (Machine) -> Unit): MachineRuleBuilder {
        return MachineRuleBuilder(this, action);
    }
    fun use(action: (MachineGroup) -> Unit): MachineGroupRuleBuilder {
        return MachineGroupRuleBuilder(this, action);
    }
}
