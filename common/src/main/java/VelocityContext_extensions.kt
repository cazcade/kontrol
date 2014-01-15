package kontrol.common


import org.apache.velocity.VelocityContext

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */

public fun <T> VelocityContext.set(key:String,value:T ): T? {
    return this.put(key,value) as T?

}