package kontrol.api


/**
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 * @todo document.
 */
public trait MachineAction {
    fun invoke(machine: Machine): Unit
}
