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
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kontrol.api.MachineGroupState
import kontrol.api.MachineGroup
import kontrol.api.MonitorRule

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public class MockGroupMonitor() : Monitor<MachineGroupState, MachineGroup> {


    val timer: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    override fun start(target: MachineGroup, stateMachine: StateMachine<MachineGroupState, MachineGroup>, rules: List<MonitorRule<MachineGroupState, MachineGroup>>) {
        timer.scheduleWithFixedDelay({
            println("Random Transition")
            try {
                stateMachine.transition(MachineGroupState.values()[(Math.random() * MachineGroupState.values().size).toInt()])
            } catch (e: Exception) {
                e.printStackTrace();
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    override fun stop() {
        timer.shutdown();
    }


}