package kontrol.api


/**
 * Holds the FSM.
 *
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 * @todo document.
 */
public trait StateMachineRules<in E : Enum<E>, in C> {
    fun on(currentState: E?, newState: E?, action: ((C) -> Unit)?): StateMachineRules<E, C>
    fun allow(currentState: E, newState: E): StateMachineRules<E, C>;

}
