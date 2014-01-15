package kontrol.sensor

import kontrol.api.LoadSensor
import kontrol.api.Machine
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.IOUtils
import net.schmizz.sshj.connection.channel.direct.Session
import java.util.concurrent.TimeUnit
import net.schmizz.sshj.connection.channel.direct.Session.Command
import kontrol.api.sensor.SensorValue

/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */
public class SSHLoadSensor : LoadSensor{
    override fun name(): String {
        return "load";
    }

    override fun start() {
    }

    override fun stop() {
    }


    override fun value(machine: Machine): SensorValue<Double?> {
        try {
            val ssh: SSHClient = SSHClient();
            ssh.addHostKeyVerifier { a, b, c -> true };
            ssh.connect(machine.ip());
            try {
                ssh.authPublickey("root");
                val session: Session? = ssh.startSession();
                try {
                    val cmd: Command? = session?.exec("cat /proc/loadavg | cut -d' ' -f1 | tr -d ' '");
                    val load = IOUtils.readFully(cmd?.getInputStream()).toString().toDouble()
                    cmd?.join(5, TimeUnit.SECONDS);
                    return SensorValue(load);
                } finally {
                    session?.close();
                }
            } finally {
                ssh.disconnect();
            }
        } catch (e: Exception) {
            println("SSHLoadSensor: ${e.javaClass} for ${machine.name()}")
            return  SensorValue(null);
        }
    }
}