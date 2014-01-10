package kontrol.impl

import kontrol.api.StateMachineRules
import java.util.ArrayList
import kontrol.impl.DefaultStateMachineRules.Rule

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public class DefaultStateMachineRules<E : Enum<E>, C> () : StateMachineRules<E, C>{
    val rules = ArrayList<Rule>();


    public inner data class Rule(val currentState: E?, val newState: E?, val action: ((C) -> Unit)?) {

    }


    fun rules(): List<Rule> {
        return rules;
    }

    override fun on(currentState: E?, newState: E?, action: ((C) -> Unit)?): StateMachineRules<E, C> {
        rules.add(Rule(currentState, newState, action));
        return this;
    }


    override fun allow(currentState: E, newState: E): StateMachineRules<E, C> {
        rules.add(Rule(currentState, newState, null));
        return this;
    }
}