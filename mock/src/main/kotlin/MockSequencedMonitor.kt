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

package kontrol.mock

import kontrol.api.Monitor
import kontrol.api.StateMachine
import kontrol.api.Machine
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.Executors
import kontrol.api.MachineState
import kontrol.api.MonitorRule

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public class MockSequencedMonitor(val states: List<MachineState?>) : Monitor<MachineState, Machine> {
    override fun target(): Machine? {
        return null;
    }


    val timer: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    override fun start(target: Machine, stateMachine: StateMachine<MachineState>, rules: Set<MonitorRule<MachineState, Machine>>) {
        println("Sequenced Transition")
        for (count in 0..states.size - 1) {
            stateMachine.transition(states[count])
        }
    }

    override fun stop() {
        timer.shutdown();
    }


}