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
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public trait Monitor<E : Enum<E>, T : Monitorable<E>> {

    fun start(target: T, stateMachine: StateMachine<E>, rules: Set<MonitorRule<E, T>>);

    /**
     * Called periodically after evaluating the monitor rules
     */
    fun heartbeat() {

    }

    /**
     * Called periodically to update the targets details.  Must be thread safe.
     */
    fun update() {

    }

    fun stop();

    fun target(): T?
}