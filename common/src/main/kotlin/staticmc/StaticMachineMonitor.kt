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

import kontrol.api.MachineState
import kontrol.api.Monitor
import kontrol.api.StateMachine
import kontrol.api.Machine
import kontrol.api.MonitorRule
import kontrol.api.Controller
import kontrol.common.BoundedComparableTemporalCollection
import kontrol.digitalocean.StaticMachine

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public  class StaticMachineMonitor(val target: StaticMachine, val stateMachine: StateMachine<MachineState>, val controller: Controller) : Monitor<MachineState, Machine>{
    override fun target(): Machine {

        return target;
    }
    override fun start(target: Machine, stateMachine: StateMachine<MachineState>, rules: Set<MonitorRule<MachineState, Machine>>) {
        controller.addMachineMonitor(this, target, rules)
    }


    override fun heartbeat() {
        //        when (target.droplet.status?.toLowerCase()) {
        //            "off" -> stateMachine.attemptTransition(MachineState.STOPPED)
        //            "new" -> stateMachine.attemptTransition(MachineState.RESTARTING)
        //            "archive" -> stateMachine.attemptTransition(MachineState.STOPPING)
        //        }
    }
    override fun update() {
        if (target.state() !in listOf(MachineState.REPAIR, MachineState.STOPPED)) {
            target.sensorArray.values(target).entrySet().forEach {
                if (!target.data.containsKey(it.key)) {
                    target.data.putIfAbsent(it.key, BoundedComparableTemporalCollection())
                }
                target.data[it.key]?.add(it.value);
            }
        }
    }

    override fun stop() {
        controller.removeMachineMonitor(this)
    }
}