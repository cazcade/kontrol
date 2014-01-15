package kontrol.api

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public trait Monitorable<E : Enum<E>, T : Monitorable<E, T>> {
    val stateMachine: StateMachine<E, T>
    var enabled: Boolean
    fun name(): String

}