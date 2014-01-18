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

package kontrol.digitalocean

import kontrol.api.MachineState
import kontrol.api.Monitor
import kontrol.api.StateMachine
import java.util.Timer
import kotlin.concurrent.*;
import kontrol.api.Machine
import kontrol.api.MonitorRule

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public  class DigitalOceanMachineMonitor(val clientFactory: DigitalOceanClientFactory) : Monitor<MachineState, Machine>{
    val timer = Timer("DOGroupMon", true);

    override fun start(target: Machine, stateMachine: StateMachine<MachineState, Machine>, monitorRules: Set<MonitorRule<MachineState, Machine>>) {
        //        println(stateMachine.rules)
        timer.schedule(500, 5000) {

            stateMachine.attemptTransition(
                    when ((target as DigitalOceanMachine).droplet.status?.toLowerCase()) {
                        "active" -> MachineState.STARTING
                        "off" -> MachineState.STOPPED
                        "new" -> MachineState.STARTING
                        "archive" -> MachineState.STOPPING
                        else -> null

                    })

            monitorRules.forEach { it.evaluate(target) }

        }

    }


    override fun stop() {
        timer.cancel();
    }
}