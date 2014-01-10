package kontrol.api

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public trait HasStateMachine<E : Enum<E>, T : HasStateMachine<E, T>> {
    val stateMachine: StateMachine<E, T>
    fun name(): String

}