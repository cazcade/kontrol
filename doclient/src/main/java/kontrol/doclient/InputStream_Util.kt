package kontrol.doclient

import java.io.InputStream
import org.apache.commons.io.IOUtils


/**
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 * @todo document.
 */


fun InputStream.readFully(encoding: String = "utf-8"): String {
    try  {
        val result = String(this.readBytes(), encoding)
        return result
    } finally {
        IOUtils.closeQuietly(this)
    }
}