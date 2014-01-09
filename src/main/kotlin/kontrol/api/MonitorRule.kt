package kontrol.api

import java.util.concurrent.ConcurrentMap
import java.util.concurrent.ConcurrentHashMap

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public  class MonitorRule<E : Enum<E>, T : HasStateMachine<E, T>>(val state: E,
                                                                  val eval: (T) -> Boolean,
                                                                  val confirms: Int,
                                                                  val name: String,
                                                                  val previousStates: List<E?>) {
    val confirmations: ConcurrentMap<T, Int> = ConcurrentHashMap();

    fun evaluate(target: T) {
        if (eval(target)) {
            val count = (confirmations.get(target)?:0).inc() ;
            confirmations.put(target, count);
            if (count >= confirms ) {
                confirmations.put(target, 0);
                if (previousStates.size() == 0 || target.stateMachine.state() in previousStates) {
                    target.stateMachine.transition(state)
                    println("Rule '$name' triggered $state on ${target.name()} after $confirms confirms")
                } else {
                    if (target.stateMachine.state() != state) {
                        //                                println("$name could not trigger $state on ${target} from state was ${target.stateMachine.state()} and allowed previous states are ${previousStates}")
                    }
                }
            }

        } else {
            confirmations.put(target, 0);
        }

    }
}