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

import kontrol.api.StateMachineRules
import java.util.ArrayList
import kontrol.common.DefaultStateMachineRules.Rule

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public class DefaultStateMachineRules<E : Enum<E>> () : StateMachineRules<E>{
    val rules = ArrayList<Rule<*>>();


    public inner data class Rule<C>(val currentState: E?, val newState: E?, val action: ((C) -> Unit)?) {

    }


    fun rules(): List<Rule<*>> {
        return rules;
    }

    override fun <C> on(currentState: E?, newState: E?, action: ((C) -> Unit)?): StateMachineRules<E> {
        rules.add(Rule(currentState, newState, action));
        return this;
    }


    override fun allow(currentState: E, newState: E): StateMachineRules<E> {
        rules.add(Rule(currentState, newState, null));
        return this;
    }
}