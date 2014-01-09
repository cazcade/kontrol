package kontrol.api


/**
 *
 * The implementation specific part.
 *
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 * @todo document.
 */
public trait Infrastructure {

    fun topology(): Topology

    fun stop() {
        topology().stop();
    }

    fun start() {
        topology().start();
    }
}
