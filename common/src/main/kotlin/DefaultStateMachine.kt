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


import kontrol.api.StateMachine
import kontrol.api.StateMachineRules
import kontrol.common.DefaultStateMachineRules.Rule
import kontrol.api.TemporalCollection

/**
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 * @todo document.
 */
public open class DefaultStateMachine<E : Enum<E>>(val target: Any) : StateMachine<E> {
    override val history: TemporalCollection<E> = BoundedTemporalCollection(100)


    override fun attemptTransition(newState: E?): StateMachine<E> {
        return transitionInternal(newState, false);
    }
    override fun state(): E? {
        return currentState;
    }

    override public var rules: StateMachineRules<E>? = null
        set(newVal) {
            if (newVal == null) {
                throw  IllegalArgumentException("Cannot set new rules value to null");
            } else {
            }

            $rules = newVal;
        }
    var currentState: E? = null;

    override fun force(newState: E?): StateMachine<E> {
        throw UnsupportedOperationException()
    }

    override fun transition(newState: E?): StateMachine<E> {
        return transitionInternal(newState, true);
    }

    fun transitionInternal(newState: E?, error: Boolean): StateMachine<E> {
        if (rules == null) {
            throw  IllegalStateException("Cannot transition with null rule set")
        }
        synchronized(this) {
            var previousState = currentState;
            currentState = newState;
            val ruleList: List<Rule<out Any?>> = (rules as DefaultStateMachineRules).rules() ;
            var okay = false ;
            //                println((rules as DefaultStateMachineRules?)?.rules?.size)
            ruleList.forEach {
                if (it.newState == newState) {
                    if (it.currentState == previousState) {
                        okay = true;
                    }
                    if ( (it.currentState == null) || (it.currentState == previousState)) {
                        val action = it.action
                        if (action != null) {
                            try {
                                action!!(target);
                            } catch (e: Exception) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
            if (!okay && previousState != null && newState != null && previousState != newState) {
                //revert to previous state
                currentState = previousState;
                if (error) {
                    throw IllegalStateException("Could not transition to ${newState} from ${currentState}");
                }
            } else {
                history.addNullable(newState);
                if (previousState != newState) {
                    //                    println("${previousState} -> ${currentState} for ${target.name()}");
                }

            }
        }


        return this;

    }


}
