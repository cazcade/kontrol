/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */

import org.junit.Test as test
import org.junit.Before as before
import org.junit.After as after
import java.io.File
import kontrol.common.scp
import kontrol.common.onHost
import kotlin.test.assertEquals

public class ExtensionsTests {

    test fun testScp() {
        var file= File("/tmp/test.scp.extension.txt")
        val testValue = "Testing 1 2 3"
        file.writeText(testValue)
        file.scp(host="teamcity.cazcade.com",path="/tmp/test.scp.extension.txt.receipt", user="root")
        assertEquals(testValue, "cat /tmp/test.scp.extension.txt.receipt".onHost("teamcity.cazcade.com"))

    }

    after before fun clean() {
        File("/tmp/test.scp.extension.txt.receipt").delete()
        "rm /tmp/test.scp.extension.txt.receipt".onHost("teamcity.cazcade.com")
    }
}