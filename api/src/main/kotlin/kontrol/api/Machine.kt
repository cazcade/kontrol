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
import java.io.Serializable


/**
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 * @todo document.
 */
public trait Machine : Monitorable<MachineState>, Serializable {

    var disableAction: ((Machine) -> Unit)?;
    var enableAction: ((Machine) -> Unit)?;
    var data: ConcurrentMap<String, ComparableTemporalStore<SensorValue>>;

    fun latestDataValues(): Map<String, SensorValue?> {
        return data.mapValues { it.value.lastEntry() }
    }

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
    override fun id(): String {
        return ip() ?: "";
    }

    fun get(s: String): SensorValue? {
        return data[s]?.lastEntry();
    }

    fun get(s: String, window: Long): List<Double?> {
        return (data[s]!! within (0..window)).map { it?.D() };
    }

    override fun transition(state: MachineState) {
        fsm.transition(state)
    }

    override fun state(): MachineState? {
        return fsm.state();
    }

    fun costPerHourInDollars(): Double
    fun costPerMonthInDollars(): Double = costPerHourInDollars() * 24 * 30


    var monitor: Monitor<MachineState, Machine>;

    fun startMonitoring(rules: Set<MonitorRule<MachineState, Machine>>) {
        println("Started monitoring ${ip()}");
        if (fsm.rules == null) {
            throw  IllegalArgumentException("Cannot monitor ${name()} without state machine rules.")
        }
        monitor.start(this, fsm, rules);
    }

    fun stopMonitoring() {
        println("Stopped monitoring ${ip()}");
        monitor.stop();
    }

    val fsm: StateMachine<MachineState>;

    override fun toString(): String {
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
