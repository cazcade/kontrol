/**
 * @todo document.
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */

package kontrol.test

import org.junit.Test as test
import org.junit.Before as before
import org.junit.After as after
import kotlin.cfclient.CloudFlareClient
import kontrol.cfclient.DomainRecord
import kotlin.test.assertEquals


public class CFClientTest {

    public test fun testGetZones() {

        val zones = CloudFlareClient("neil@cazcade.com", "ec591f8e5dc01a9bb9bd56b01c69ce107d82d").zones()
        println(zones)
        println(zones[0].display_name)

        val records = CloudFlareClient("neil@cazcade.com", "ec591f8e5dc01a9bb9bd56b01c69ce107d82d").domainRecords("pinstamatic.com").filter { it.display_name == "cfclienttest" }
        println(records)
        println(records[0].display_name)
        val initial = CloudFlareClient("neil@cazcade.com", "ec591f8e5dc01a9bb9bd56b01c69ce107d82d").update("pinstamatic.com", DomainRecord(`type` = "A", name = "cfclienttest", content = "127.0.0.1", ttl = "1", rec_id=records[0].rec_id))
        assertEquals("127.0.0.1",initial.content)

//        val record = CloudFlareClient("neil@cazcade.com", "ec591f8e5dc01a9bb9bd56b01c69ce107d82d").create("pinstamatic.com", DomainRecord(`type` = "A", name = "cfclienttest", content = "127.0.0.1", ttl = "1"))
//        println(record)

        val record = CloudFlareClient("neil@cazcade.com", "ec591f8e5dc01a9bb9bd56b01c69ce107d82d").update("pinstamatic.com", DomainRecord(content = "127.0.0.2", name = "cfclienttest", `type` = "A", ttl="1",rec_id=records[0].rec_id))
        assertEquals("127.0.0.2",record.content)
        val changedRecord = CloudFlareClient("neil@cazcade.com", "ec591f8e5dc01a9bb9bd56b01c69ce107d82d").domainRecords("pinstamatic.com").filter { it.display_name == "cfclienttest" }[0]
        assertEquals("127.0.0.2",changedRecord.content)


    }
}



