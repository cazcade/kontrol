package kontrol.api

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public trait Monitor<E : Enum<E>, T : Monitorable<E, T>> {

    fun start(target: T, stateMachine: StateMachine<E, T>, rules: List<MonitorRule<E, T>>);
    fun stop();
}