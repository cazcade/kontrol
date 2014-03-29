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
import java.util.concurrent.ConcurrentHashMap

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public  class MonitorRule<E : Enum<E>, T : Monitorable<E>>(val state: E?,
                                                           val eval: (T) -> Boolean,
                                                           val confirms: Int,
                                                           val name: String,
                                                           val previousStates: Set<E?>) : Comparable<MonitorRule<E, T>>{
    override public fun compareTo(other: MonitorRule<E, T>): Int {
        return toString().compareTo(other.toString())
    }

    val confirmations: ConcurrentMap<String, Int> = ConcurrentHashMap();

    fun evaluate(target: T, eventLog: EventLog) {
        if (!target.enabled) {
            println("${target.name()} was disabled")
            return
        }
        if (eval(target)) {
            if (previousStates.size() == 0 || target.state() in previousStates) {
                val count = (confirmations.get(target.id())?:0).inc() ;
                if (count >= confirms ) {
                    confirmations.put(target.id(), 0);
                    eventLog.log(target.name(), state, LogContextualState.TRIGGER, "Rule '$name' triggered $state on ${target.name()} after $confirms confirms")
                    target.transition(state)
                    println("Rule '$name' triggered $state after $confirms confirms")
                } else {
                    confirmations.put(target.id(), count);
                }
            } else {
                if (target.state() != state) {
                    //                                println("$name could not trigger $state on ${target} from state was ${target.stateMachine.state()} and allowed previous states are ${previousStates}")
                }
            }

        } else {
            confirmations.put(target.id(), 0);
        }

    }


    override fun toString(): String {
        val stringBuilder = StringBuilder()
        previousStates.appendString(stringBuilder)
        return "$name=${stringBuilder}->$state";
    }

    override fun hashCode(): Int {
        return toString().hashCode();
    }

    override fun equals(other: Any?): Boolean {
        return if (other is MonitorRule<*, *>) {
            toString() == other.toString();
        } else {
            false
        }
    }

}