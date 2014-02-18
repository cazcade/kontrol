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

package kontrol.examples.docean

import kontrol.digitalocean.DigitalOceanMachineGroup
import kontrol.digitalocean.DigitalOceanClientFactory
import kontrol.sensor.SSHLoadSensor
import kontrol.sensor.HttpStatusSensor
import kontrol.digitalocean.DigitalOceanConfig
import kontrol.sensor.HttpLoadSensor
import kontrol.common.DefaultSensorArray
import kontrol.api.Infrastructure
import kontrol.sensor.HttpResponseTimeSensor
import kontrol.postmortem.CentosPostmortem
import kontrol.postmortem.JettyPostmortem
import kontrol.server.Server
import kontrol.api.Controller
import kontrol.common.group.ext.addGroupSensorRules
import kontrol.common.group.ext.addMachineOverloadRules
import kontrol.digitalocean.StaticMachine
import java.util.ArrayList
import kontrol.api.sensors.SensorArray
import kontrol.api.MachineGroup
import kontrol.staticmc.MixedCloud
import kontrol.common.group.ext.allowDefaultTransitions
import kontrol.common.group.ext.applyDefaultPolicies
import kontrol.common.group.ext.applyDefaultRules
import kontrol.common.group.ext.addMachineBrokenRules
import kontrol.konfigurators.HaproxyKonfigurator
import kontrol.staticmc.StaticMachineGroup
import kontrol.ext.string.ssh.onHost
import kontrol.api.OS


public fun snapitoSensorActions(infra: Infrastructure): Infrastructure {
    infra.topology().each { group ->
        group.addMachineBrokenRules("http-status" to 399.0)
        when(group.name()) {
            "lb", "gateway" -> {
                group.addMachineOverloadRules("load" to 6.0, "http-response-time" to 500.0)
                group.addGroupSensorRules("http-response-time" to -1.0..200.0, "load" to 2.0..5.0)
            }
            "worker", "static-worker" -> {
                //change this when deployed
                group.addMachineOverloadRules("load" to 10.0, "http-response-time" to 5000.0)
                group.addGroupSensorRules("http-response-time" to -1.0..2000.0, "http-load" to 4.0..10.0)
            }
        }
    }
    return infra;
}

fun staticMachines(groupName: String, controller: Controller sensors: SensorArray, vararg mcs: Pair<String, String>): List<StaticMachine> {
    val list: ArrayList<StaticMachine> = arrayListOf()
    for (mc in mcs) {
        list.add(StaticMachine(sensors, controller, groupName, 109.0 / 24 / 36, mc.second, mc.second, mc.second, mc.first))
    }
    return list;

}


fun buildGroups(client: DigitalOceanClientFactory, controller: Controller, test: Boolean = true): Map<String, MachineGroup> {
    val cloudFlareKonfigurator = CloudFlareKonfigurator(System.getProperty("cf.email")?:"", System.getProperty("cf.apikey")?:"", "snapito.com", if (test) "test.api" else "api")

    val gatewaySensorArray = DefaultSensorArray(listOf(SSHLoadSensor(), HttpStatusSensor("/gateway?status"), HttpResponseTimeSensor("/gateway?status")));
    val loadBalancerSensorArray = DefaultSensorArray(listOf(SSHLoadSensor(), HttpStatusSensor("/_stats", 8888), HttpResponseTimeSensor("/_stats", 8888)));
    val workerSensorArray = DefaultSensorArray(listOf(SSHLoadSensor(), HttpLoadSensor("/api/load"), HttpStatusSensor("/api?key=monitor&url=google.com&freshness=1"), HttpResponseTimeSensor("/api?key=monitor&url=google.com&freshness=1")));
    val staticWorkerSensorArray = DefaultSensorArray(listOf(SSHLoadSensor("administrator", OS.OSX), HttpLoadSensor("/api/load"), HttpStatusSensor("/api?url=google.com&freshness=60&key=monitor"), HttpResponseTimeSensor("/api?url=google.com&freshness=1&key=monitor")));
    val lbConfig = DigitalOceanConfig(if (test) "test-snapito-" else "prod-snapito-", "template-snapito-", 4, 66)
    val gatewayConfig = DigitalOceanConfig(if (test) "test-snapito-" else "prod-snapito-", "template-snapito-", 4, 62)
    val workerConfig = DigitalOceanConfig(if (test) "test-snapito-" else "prod-snapito-", "template-snapito-", 4, 65)

    val keys = "Neil Laptop,Eric New"


    val loadBalancerGroup = DigitalOceanMachineGroup(client, controller, "lb", loadBalancerSensorArray, lbConfig, keys, 2, 2, 5, arrayListOf(), listOf(CentosPostmortem()), downStreamKonfigurator = HaproxyKonfigurator("/haproxy.cfg.vm"), upStreamKonfigurator = cloudFlareKonfigurator
    )

    val gatewayGroup = DigitalOceanMachineGroup(client, controller, "gateway", gatewaySensorArray, gatewayConfig, keys, 2, 2, 5, arrayListOf(loadBalancerGroup), listOf(CentosPostmortem(), JettyPostmortem("/home/cazcade/jetty")))

    val workerGroup = DigitalOceanMachineGroup(client, controller, "worker", workerSensorArray, workerConfig, keys, 0, 0, 0, arrayListOf<MachineGroup>(), listOf(CentosPostmortem(), JettyPostmortem("/home/cazcade/jetty")), upStreamKonfigurator = WorkerKonfigurator())


    val staticWorkerGroup = StaticMachineGroup(staticMachines("static-worker", controller, staticWorkerSensorArray, "worker-1" to "worker-208-52-161-6.snapito.com", "worker-2" to "worker-208-52-168-80.snapito.com", "worker-3" to "worker-208-52-182-75.snapito.com", "worker-4" to "worker-208-52-190-173.snapito.com", "worker-5" to "worker-208-52-191-139.snapito.com"), "killall java;killall xulrunner;killall phantomjs;rm -rf /tmp/*; rm -rf ~/cazcade/images/*", "sudo reboot", "administrator", controller, "static-worker", staticWorkerSensorArray, keys, arrayListOf(), arrayListOf(), upStreamKonfigurator = WorkerKonfigurator("administrator"))


    val pinstamaticCloudFlareKonfigurator = CloudFlareKonfigurator(System.getProperty("cf.email")?:"", System.getProperty("cf.apikey")?:"", "pinstamatic.com", if (test) "test.pinstamatic.com" else "pinstamatic.com")
    val pinstamaticGroup = DigitalOceanMachineGroup(client, controller, "pinstamatic", DefaultSensorArray(listOf(SSHLoadSensor(), HttpStatusSensor("/pinstamatic-api/snap?preview&url=http%3A%2F%2Fpinstamatic.com%2Fcontent-text-sticky.html%3Ftext%3Dtest%26color%3Dyellow"), HttpResponseTimeSensor("/pinstamatic-api/snap?preview&url=http%3A%2F%2Fpinstamatic.com%2Fcontent-text-sticky.html%3Ftext%3Dtest%26color%3Dyellow"))), DigitalOceanConfig(if (test) "test-" else "", "template-snapito-", 4, 62), keys, 1, 1, 1, arrayListOf<MachineGroup>(), listOf(CentosPostmortem(), JettyPostmortem("/home/cazcade/jetty")), upStreamKonfigurator = pinstamaticCloudFlareKonfigurator)



    return hashMapOf(
            "worker" to workerGroup,
            "lb" to loadBalancerGroup,
            "gateway" to gatewayGroup,
            "static-worker" to staticWorkerGroup,
            "pinstamatic" to  pinstamaticGroup

    );
}


fun main(args: Array<String>): Unit {


    val server = Server { controller, bus, postmortems ->
        val cient = DigitalOceanClientFactory(System.getProperty("do.cid")?:"", System.getProperty("do.apikey")?:"")
        val cloud = MixedCloud(buildGroups(cient, controller, false))
        cloud.topology().each { group ->
            group.allowDefaultTransitions();
        }
        cloud["pinstamatic"].applyDefaultPolicies(controller, postmortems);
        cloud["gateway"].applyDefaultPolicies(controller, postmortems, { m, g ->
            " su  cazcade -c 'cd ~/snapito; git checkout prod; git pull; mvn clean install -Dmaven.test.skip=true; killall java; rm -rf /tmp/*.png;sleep 30; ~/jetty/bin/jetty.sh start;sleep 180'".onHost(m.ip())
        }, { m, g ->
            "su cazcade -c 'cd ~/snapito; git checkout master; git pull; mvn clean install -Dmaven.test.skip=true; killall java; rm -rf /tmp/*.png; ~/jetty/bin/jetty.sh restart'".onHost(m.ip())
        })
        cloud["lb"].applyDefaultPolicies(controller, postmortems, { m, g -> g.configure(m) })
        cloud["static-worker"].applyDefaultPolicies(controller, postmortems, { m, g ->
            " cd app/snapito; git checkout prod; git pull; mvn clean install -Dmaven.test.skip=true; killall java; cp -f ./snapito-api/target/snapito-api-1.0-SNAPSHOT.war /usr/local/tomcat/webapps/api.war; rm -rf /tmp/*.png; sleep 180".onHost(m.ip(), "administrator")
        },
                { m, g ->
                    " cd app/snapito; git checkout master; git pull; mvn clean install -Dmaven.test.skip=true; killall java;cp -f ./snapito-api/target/snapito-api-1.0-SNAPSHOT.war /usr/local/tomcat/webapps/api.war; rm -rf /tmp/*.png; sleep 120".onHost(m.ip(), "administrator")
                }
        )
        cloud["worker"].applyDefaultPolicies(controller, postmortems, { m, g ->
            " su cazcade -c 'cd ~/snapito; git checkout prod; git pull; mvn clean install -Dmaven.test.skip=true; killall java; rm -rf /tmp/*.png; ~/jetty/bin/jetty.sh restart; sleep 120'".onHost(m.ip())
        }, { m, g ->
            " su cazcade -c 'cd ~/snapito; git checkout master; git pull; mvn clean install -Dmaven.test.skip=true; killall java; rm -rf /tmp/*.png; ~/jetty/bin/jetty.sh restart; sleep 120'".onHost(m.ip())
        })

        cloud["lb"].applyDefaultRules(60);
        cloud["worker"].applyDefaultRules(60);
        cloud["static-worker"].applyDefaultRules(60);
        cloud["gateway"].applyDefaultRules(60);
        cloud["pinstamatic"].applyDefaultRules(60);

        //        cloud["lb"] becomes GROUP_BROKEN ifStateIn listOf(QUIET, BUSY, NORMAL, null) andTest {
        //            cloud["gateway"].state() != GROUP_BROKEN && cloud["worker"].state() != GROUP_BROKEN && it.machines().size < it.hardMax
        //            HttpUtil.getStatus(URI("http://api.snapito.com/web/monitor/lc/sky.com?freshness=1"), Locale.getDefault(), 60000) >= 400
        //        } after 60 seconds "api-broken"

        snapitoSensorActions(cloud);
    }

    server.start()
    try {

        while (true) {
            Thread.sleep(10000)
        }
    } finally  {
        server.stop();
    }

}


