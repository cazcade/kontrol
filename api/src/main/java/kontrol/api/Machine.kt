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

import java.util.concurrent.ConcurrentMap
import kontrol.api.sensor.SensorValue


/**
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 * @todo document.
 */
public trait Machine : Monitorable<MachineState, Machine> {

    var disableAction: ((Machine) -> Unit)?;
    var enableAction: ((Machine) -> Unit)?;
    var data: ConcurrentMap<String, SensorValue<Any?>>;

    fun ip(): String?


    /**
     * Override this.
     */
    override fun name(): String {
        return ip() ?: "";
    }


    /**
     * Override this.
     */
    fun privateIp(): String? {
        return ip()
    }


    /**
     * Override this.
     */
    fun hostname(): String {
        return ip() ?: "";
    }


    /**
     * Override this.
     */
    fun id(): String {
        return ip() ?: "";
    }

    fun get(s: String): SensorValue<Any?>? {
        return data[s];
    }


    fun state(): MachineState? {
        return stateMachine.state();
    }

    var monitor: Monitor<MachineState, Machine>;

    fun startMonitoring(rules: List<MonitorRule<MachineState, Machine>>) {
        println("Started monitoring ${ip()}");
        if (stateMachine.rules == null) {
            throw  IllegalArgumentException("Cannot monitor ${name()} without state machine rules.")
        }
        monitor.start(this, stateMachine, rules);
    }

    fun stopMonitoring() {
        println("Stopped monitoring ${ip()}");
        monitor.stop();
    }

    override val stateMachine: StateMachine<MachineState, Machine>;

    fun toString(): String {
        return "${name()}@${ip()} (${id()}) [${state()}]  - ${data}";
    }

    fun disable() {
        enabled = false;
        if (disableAction != null) {
            disableAction!!(this)
        };
    }

    fun enable() {
        enabled = true;
        if (enableAction != null) {
            enableAction!!(this)
        };
    }


}
