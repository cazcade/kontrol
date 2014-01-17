/*
 * Copyright 2014 Cazcade Limited (http://cazcade.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        val initial = CloudFlareClient("neil@cazcade.com", "ec591f8e5dc01a9bb9bd56b01c69ce107d82d").update("pinstamatic.com", DomainRecord(`type` = "A", name = "cfclienttest", content = "127.0.0.1", ttl = "1", rec_id = records[0].rec_id))
        assertEquals("127.0.0.1", initial.content)

        //        val record = CloudFlareClient("neil@cazcade.com", "ec591f8e5dc01a9bb9bd56b01c69ce107d82d").create("pinstamatic.com", DomainRecord(`type` = "A", name = "cfclienttest", content = "127.0.0.1", ttl = "1"))
        //        println(record)

        val record = CloudFlareClient("neil@cazcade.com", "ec591f8e5dc01a9bb9bd56b01c69ce107d82d").update("pinstamatic.com", DomainRecord(content = "127.0.0.2", name = "cfclienttest", `type` = "A", ttl = "1", rec_id = records[0].rec_id))
        assertEquals("127.0.0.2", record.content)
        val changedRecord = CloudFlareClient("neil@cazcade.com", "ec591f8e5dc01a9bb9bd56b01c69ce107d82d").domainRecords("pinstamatic.com").filter { it.display_name == "cfclienttest" }[0]
        assertEquals("127.0.0.2", changedRecord.content)


    }
}



