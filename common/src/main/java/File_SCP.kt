package kontrol.common

import net.schmizz.sshj.SSHClient
import java.io.File

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */

fun File.scp(host: String? = "localhost", user: String = "root", path: String = "~/", retry: Int = 3) {
    if (host == null) {
        return
    }
    val ssh: SSHClient = SSHClient();
    ssh.addHostKeyVerifier { a, b, c -> true };

    for ( i in 1..retry) {
        try {
            ssh.connect(host);
            try {
                ssh.authPublickey(user);
                val scp = ssh.newSCPFileTransfer()!!
                scp.upload(this.getAbsolutePath(), path)
            } finally {
                ssh.disconnect();
            }
        } catch (e: Exception) {
            println("Retrying for the $i time to scp to $user@$host/$path - ${e.getMessage()}")
            Thread.sleep(100)
        }
        return
    }
}
