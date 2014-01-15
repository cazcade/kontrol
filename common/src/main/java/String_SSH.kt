package kontrol.common


import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.connection.channel.direct.Session.Command
import java.util.concurrent.TimeUnit

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */

fun String.onHost(host: String? = "localhost", user: String = "root", retry:Int=3): String? {
    if(host == null) {
        return null
    }
    val ssh: SSHClient = SSHClient();
    ssh.addHostKeyVerifier { a, b, c -> true };
    for ( i in 1..retry) {
        try {
            ssh.connect(host);
            try {
                ssh.authPublickey(user);
                val session: Session = ssh.startSession()!!
                try {
                    val cmd = session.exec(this)!!;
                    val ins = cmd.getInputStream()
                    return if (ins != null) {
                        val result = String(ins.readBytes(2048))
                        cmd.join(5, TimeUnit.SECONDS);
                        result;
                    } else {
                        null
                    }
                } finally {
                    session.close();
                }
            } finally {
                ssh.disconnect();
            }
        } catch (e: Exception) {
            println("Retrying for the $i time to ssh $user@$host -c $this  (${e.getMessage()})")
            Thread.sleep(100)
        }
    }
    return null
}