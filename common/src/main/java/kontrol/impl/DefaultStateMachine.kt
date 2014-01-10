package kontrol.impl

import kontrol.api.StateMachine
import kontrol.api.StateMachineRules
import kontrol.api.HasStateMachine

/**
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 * @todo document.
 */
public open class DefaultStateMachine<E : Enum<E>, T : HasStateMachine<E, T>>(val target: T) : StateMachine<E, T> {
    override fun attemptTransition(newState: E?): StateMachine<E, T> {
        return transitionInternal(newState, false);
    }
    override fun state(): E? {
        return currentState;
    }
    override public var rules: StateMachineRules<E, T>? = null
        set(newVal) {
            if (newVal == null) {
                throw  IllegalArgumentException("Cannot set new rules value to null");
            } else {
            }

            $rules = newVal;
        }
    var currentState: E? = null;

    override fun force(newState: E?): StateMachine<E, T> {
        throw UnsupportedOperationException()
    }

    override fun transition(newState: E?): StateMachine<E, T> {
        return transitionInternal(newState, true);
    }

    fun transitionInternal(newState: E?, error: Boolean): StateMachine<E, T> {
        if (rules == null) {
            throw  IllegalStateException("Cannot transition with null rule set")
        }
        synchronized(this) {
            var previousState = currentState;
            currentState = newState;
            val ruleList = (rules as DefaultStateMachineRules?)?.rules() ;
            var okay = false ;
            //                println((rules as DefaultStateMachineRules?)?.rules?.size)
            ruleList?.forEach {
                if (it.newState == newState) {
                    if (it.currentState == previousState) {
                        okay = true;
                    }
                    if ( (it.currentState == null) || (it.currentState == previousState)) {
                        val action = it.action
                        if (action != null) {
                            try {
                                action(target);
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
                if (previousState != newState) {
                    //                    println("${previousState} -> ${currentState} for ${target.name()}");
                }

            }
        }


        return this;

    }


}
