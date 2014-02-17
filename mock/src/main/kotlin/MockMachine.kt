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

import kontrol.api.Machine
import kontrol.common.DefaultStateMachine
import kontrol.api.StateMachine
import kontrol.api.Monitor
import kontrol.api.MachineState
import kontrol.api.sensor.SensorValue
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.ConcurrentHashMap
import kontrol.api.ComparableTemporalStore
import kontrol.api.Monitorable

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public class MockMachine(val ip: String) : Machine{
    override fun costPerHourInDollars(): Double {
        return 0.0;
    }
    override fun groupName(): String {
        return "mock"
    }

    override var disableAction: ((Monitorable<MachineState>) -> Unit)? = null
    override var enableAction: ((Monitorable<MachineState>) -> Unit)? = null
    override var data: ConcurrentMap<String, ComparableTemporalStore<SensorValue>> = ConcurrentHashMap();

    override var monitor: Monitor<MachineState, Machine> = MockMachineMonitor();
    override val fsm: StateMachine<MachineState> = DefaultStateMachine<MachineState>(this);
    override var enabled: Boolean = true;

    override fun ip(): String {
        return ip;
    }


}