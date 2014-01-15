package kontrol.api


/**
 * Holds the FSM.
 *
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 * @todo document.
 */
public trait StateMachine<E : Enum<E>, T : Monitorable<E, T>> {
    var rules: StateMachineRules<E, T>?;
    fun force(newState: E?): StateMachine<E, T>
    fun transition(newState: E?): StateMachine<E, T>
    fun attemptTransition(newState: E?): StateMachine<E, T>
    fun state(): E?;
}
