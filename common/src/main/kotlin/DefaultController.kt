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
import java.util.HashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public class DefaultController(val timeoutInMinutes: Long = 30) : Controller{

    val actions = HashMap<String, (Machine) -> Unit>();
    val groupActions = HashMap<String, (MachineGroup) -> Unit>();
    val executor = Executors.newSingleThreadExecutor();

    fun key(group: MachineGroup, action: Action): String {
        return "group.${group.name()}.action.${action}";
    }
    fun key(group: MachineGroup, action: GroupAction): String {
        return "group.${group.name()}.group-action.${action}";
    }

    fun key(machine: Machine, action: Action): String {
        return "machine.${machine.ip()}.action.${action}";
    }

    override fun execute(group: MachineGroup, machine: Machine, vararg actionArgs: Action): Controller {
        //        println(actions);
        actionArgs.forEach {
            val action = actions[key(machine, it)];
            if (action != null) {
                println("Performing action for $action on ${machine.ip()}")
                executor.submit { action(machine) } .get(timeoutInMinutes, TimeUnit.MINUTES)
            };
            val action2 = actions[key(group, it)];
            if (action2 != null) {
                println("Performing action for $action2 on ${machine.ip()}")
                executor.submit { action2(machine) } .get(timeoutInMinutes, TimeUnit.MINUTES)
            };
        }
        return this;
    }
    override fun execute(group: MachineGroup, vararg actionArgs: GroupAction): Controller {
        //        println(groupActions);
        actionArgs.forEach {
            val key = key(group, it)
            println(key)
            val action = groupActions[key];
            if (action != null) {
                println("Performing action for $action on ${it.name()}")
                executor.submit { action(group) }.get(timeoutInMinutes, TimeUnit.MINUTES)
            };
        }
        return this;
    }

    override fun execute(group: MachineGroup, vararg actionArgs: Action): Controller {
        println(actions);

        actionArgs.forEach {
            val actionArg = it;
            val key = key(group, actionArg)
            println(key)
            val action = actions[key];
            if (action != null) {
                println("Action is not null")
                group.machines().forEach {
                    println("Performing action for $action on ${it.ip()}")
                    executor.submit { action(it) } .get(timeoutInMinutes, TimeUnit.MINUTES)
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
    override fun register(group: MachineGroup, action: Action, machineAction: (Machine) -> Unit): Controller {
        actions.put(key(group, action), machineAction);
        return this;
    }
    override fun register(machine: Machine, action: Action, machineAction: (Machine) -> Unit): Controller {
        actions.put(key(machine, action), machineAction);
        return this;
    }
    override fun register(group: MachineGroup, action: GroupAction, machineGroupAction: (MachineGroup) -> Unit): Controller {
        groupActions.put(key(group, action), machineGroupAction);
        return this;
    }


}