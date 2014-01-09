package kontrol.api


/**
 * A collection of configured watchers and monitors to apply to a Controller.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 * @todo document.
 */
public trait Strategy {
    fun applyTo(controller: Infrastructure): Unit


}
