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
import kontrol.postmortem.TomcatPostmortem
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
import kontrol.postmortem.JettyPostmortem
import kontrol.postmortem.HeapDumpsPostmortem


public fun snapitoSensorActions(infra: Infrastructure): Infrastructure {
    infra.topology().each { group ->
        group.addMachineBrokenRules("http-status" to 399.0)
        when(group.name()) {
        //            "lb", "gateway" -> {
        //                group.addMachineOverloadRules("load" to 6.0, "http-response-time" to 500.0)
        //                group.addGroupSensorRules("http-response-time" to -1.0..200.0, "load" to 2.0..5.0)
        //            }
            "static-linux-snapito.io" -> {
                group.addMachineOverloadRules("load" to 6.0, "http-response-time" to 500.0)
                group.addGroupSensorRules("http-response-time" to -1.0..200.0, "load" to 2.0..5.0)
            }
            "worker", "static-osx-workers", "static-linux-workers", "pinstamatic" -> {
                //change this when deployed
                group.addMachineOverloadRules("load" to 10.0, "http-response-time" to 5000.0)
                group.addGroupSensorRules("http-response-time" to -1.0..2000.0, "http-load" to 4.0..10.0)
            }
        }
    }
    return infra;
}

fun staticMachines(groupName: String, controller: Controller sensors: SensorArray, cost: Double, vararg mcs: Pair<String, String>): List<StaticMachine> {
    val list: ArrayList<StaticMachine> = arrayListOf()
    for (mc in mcs) {
        list.add(StaticMachine(sensors, controller, groupName, cost / 24 / 30.5, mc.second, mc.second, mc.second, mc.first))
    }
    return list;

}


fun buildGroups(client: DigitalOceanClientFactory, controller: Controller, test: Boolean = true): Map<String, MachineGroup> {
    val cloudFlareKonfigurator = CloudFlareKonfigurator(System.getProperty("cf.email")?:"", System.getProperty("cf.apikey")?:"", "snapito.com", if (test) "test.api" else "api")

    val gatewaySensorArray = DefaultSensorArray(listOf(SSHLoadSensor(), HttpStatusSensor("/gateway?status"), HttpResponseTimeSensor("/gateway?status")));
    val loadBalancerSensorArray = DefaultSensorArray(listOf(SSHLoadSensor(), HttpStatusSensor("/gateway?status"), HttpResponseTimeSensor("/_stats", 8888)));
    val workerSensorArray = DefaultSensorArray(listOf(SSHLoadSensor(), HttpLoadSensor("/api/load"), HttpStatusSensor("/api?key=monitor&url=google.com&freshness=1"), HttpResponseTimeSensor("/api?key=monitor&url=google.com&freshness=1")));
    val staticWorkerSensorArray = DefaultSensorArray(listOf(SSHLoadSensor("administrator", OS.OSX), HttpLoadSensor("/api/load"), HttpStatusSensor("/api?url=google.com&freshness=60&key=monitor"), HttpResponseTimeSensor("/api?url=google.com&freshness=1&key=monitor")));
    val lbConfig = DigitalOceanConfig(if (test) "test-snapito-" else "prod-snapito-", "template-snapito-", 4, 66)
    val gatewayConfig = DigitalOceanConfig(if (test) "test-snapito-" else "prod-snapito-", "template-snapito-", 4, 62)
    val workerConfig = DigitalOceanConfig(if (test) "test-snapito-" else "prod-snapito-", "template-snapito-", 4, 65)

    val keys = "Neil Laptop,Eric New"


    val loadBalancerGroup = DigitalOceanMachineGroup(client, controller, "lb", loadBalancerSensorArray, lbConfig, keys, 2, 2, 5, arrayListOf(), listOf(CentosPostmortem()), downStreamKonfigurator = HaproxyKonfigurator("/haproxy.cfg.vm"), upStreamKonfigurator = cloudFlareKonfigurator
    )

    val gatewayGroup = DigitalOceanMachineGroup(client, controller, "gateway", gatewaySensorArray, gatewayConfig, keys, 2, 2, 5, arrayListOf(loadBalancerGroup), listOf(CentosPostmortem(), JettyPostmortem("/home/cazcade/jetty"), HeapDumpsPostmortem()))

    val workerGroup = DigitalOceanMachineGroup(client, controller, "worker", workerSensorArray, workerConfig, keys, 0, 0, 0, arrayListOf<MachineGroup>(), listOf(CentosPostmortem(), JettyPostmortem("/home/cazcade/jetty"), HeapDumpsPostmortem()), upStreamKonfigurator = WorkerKonfigurator())


    val staticOSXWorkers = StaticMachineGroup(staticMachines("static-osx-workers", controller, staticWorkerSensorArray, 109.0, "osx-worker-1" to "208.52.187.175", "osx-worker-2" to "208.52.187.176", "osx-worker-3" to "208.52.187.180", "osx-worker-4" to "208.52.187.178", "osx-worker-5" to "208.52.187.179"), "killall java;killall xulrunner;killall phantomjs;rm -rf /tmp/*; rm -rf ~/cazcade/images/*", "sudo reboot", "administrator", controller, "static-osx-workers", staticWorkerSensorArray, keys, arrayListOf(), arrayListOf(TomcatPostmortem("/usr/local/tomcat"), HeapDumpsPostmortem()), upStreamKonfigurator = WorkerKonfigurator("administrator"))

    val contaboMachines = staticMachines("static-linux-workers", controller, workerSensorArray, 149.0, "contabo-worker-1" to "80.241.209.220")
    val hetznerMachines = staticMachines("static-linux-workers", controller, workerSensorArray, 149.0, "EOL!!!-hetzner-worker-1" to "144.76.194.178")
    val onlineNetMachines = staticMachines("static-linux-workers", controller, workerSensorArray, 80.0, "online.net-1" to "62.210.148.130")
    val staticLinuxMachines = contaboMachines plus hetznerMachines plus onlineNetMachines;
    val staticLinuxWorkers = StaticMachineGroup(staticLinuxMachines, "killall java;killall xulrunner;killall phantomjs;rm -rf /tmp/*; rm -rf ~/cazcade/images/*", "sudo reboot", "administrator", controller, "static-linux-workers", staticWorkerSensorArray, keys, arrayListOf(), arrayListOf(CentosPostmortem(), JettyPostmortem("/home/cazcade/jetty")))


    val snapitoIOSensorArray = DefaultSensorArray(listOf(SSHLoadSensor(), HttpStatusSensor("/"), HttpResponseTimeSensor("/")));
    val snapitoIOMachines = staticMachines("static-linux-workers", controller, workerSensorArray, 80.0, "snapito.io-new-1" to "142.4.219.14")
    val snapitoIO = StaticMachineGroup(snapitoIOMachines, "true", "sudo reboot", "administrator", controller, "static-linux-snapito.io", snapitoIOSensorArray, keys, arrayListOf(), arrayListOf(CentosPostmortem()))


    val pinstamaticCloudFlareKonfigurator = CloudFlareKonfigurator(System.getProperty("cf.email")?:"", System.getProperty("cf.apikey")?:"", "pinstamatic.com", if (test) "test.pinstamatic.com" else "pinstamatic.com")

    val pinstamaticGroup = DigitalOceanMachineGroup(client, controller, "pinstamatic.com", DefaultSensorArray(listOf(SSHLoadSensor(), HttpStatusSensor("/pinstamatic-api/snap?preview&url=http%3A%2F%2Fpinstamatic.com%2Fcontent-text-sticky.html%3Ftext%3Dtest%26color%3Dyellow"), HttpResponseTimeSensor("/pinstamatic-api/snap?preview&url=http%3A%2F%2Fpinstamatic.com%2Fcontent-text-sticky.html%3Ftext%3Dtest%26color%3Dyellow"))), DigitalOceanConfig(if (test) "test-" else "", "template-snapito-", 4, 62), keys, 2, 2, 4, arrayListOf<MachineGroup>(), listOf(CentosPostmortem(), TomcatPostmortem("/home/cazcade/jetty")), upStreamKonfigurator = pinstamaticCloudFlareKonfigurator)



    return hashMapOf(
            "worker" to workerGroup,
            //            "lb" to loadBalancerGroup,
            //            "gateway" to gatewayGroup,
            "static-osx-workers" to staticOSXWorkers,
            "static-linux-workers" to staticLinuxWorkers,
            "static-linux-snapito.io" to snapitoIO,
            "pinstamatic" to  pinstamaticGroup

    );
}


fun main(args: Array<String>): Unit {


    val server = Server { controller, bus, postmortems ->
        val cient = DigitalOceanClientFactory(System.getProperty("do.cid")?:"", System.getProperty("do.apikey")?:"")
        val cloud = MixedCloud(buildGroups(cient, controller, false))
        cloud.topology().each { group ->
            group.allowDefaultTransitions();
            group.applyDefaultRules(60);
        }
        cloud["pinstamatic"].applyDefaultPolicies(controller, postmortems);
        //        cloud["gateway"].applyDefaultPolicies(controller, postmortems, { m, g ->
        //            " su  cazcade -c 'cd ~/snapito; git checkout prod; git pull; mvn clean install -Dmaven.test.skip=true; killall java; rm -rf /tmp/*.png;sleep 30; ~/jetty/bin/jetty.sh start;sleep 180'".onHost(m.ip())
        //        }, { m, g ->
        //            "su cazcade -c 'cd ~/snapito; git checkout master; git pull; mvn clean install -Dmaven.test.skip=true; killall java; rm -rf /tmp/*.png; ~/jetty/bin/jetty.sh restart'".onHost(m.ip())
        //        })
        //        cloud["lb"].applyDefaultPolicies(controller, postmortems, { m, g -> g.configure(m) })
        cloud["static-osx-workers"].applyDefaultPolicies(controller, postmortems, { m, g ->
        " cd app/snapito; git checkout prod; git pull; mvn clean install -Dmaven.test.skip=true; killall java; cp -f ./snapito-api/target/snapito-api-1.0-SNAPSHOT.war /usr/local/tomcat/webapps/api.war; rm -rf /tmp/*.png; sleep 180".onHost(m.ip(), "administrator")
        },
                { m, g ->
                    " cd app/snapito; git checkout master; git pull; mvn clean install -Dmaven.test.skip=true; killall java;cp -f ./snapito-api/target/snapito-api-1.0-SNAPSHOT.war /usr/local/tomcat/webapps/api.war; rm -rf /tmp/*.png; sleep 120".onHost(m.ip(), "administrator")

                }
        )

        cloud["static-linux-snapito.io"].applyDefaultPolicies(controller, postmortems);
        cloud["static-linux-workers"].applyDefaultPolicies(controller, postmortems);
        cloud["worker"].applyDefaultPolicies(controller, postmortems, { m, g ->
            " su cazcade -c 'cd ~/snapito; git checkout prod; git pull; mvn clean install -Dmaven.test.skip=true; killall java; rm -rf /tmp/*.png; ~/jetty/bin/jetty.sh restart; sleep 120'".onHost(m.ip())
        }, { m, g ->
            " su cazcade -c 'cd ~/snapito; git checkout master; git pull; mvn clean install -Dmaven.test.skip=true; killall java; rm -rf /tmp/*.png; ~/jetty/bin/jetty.sh restart; sleep 120'".onHost(m.ip())
        })

        //        cloud["lb"] becomes MachineGroupState.GROUP_BROKEN ifStateIn listOf(MachineGroupState.QUIET, MachineGroupState.BUSY, MachineGroupState.NORMAL, null) andTest {
        //            cloud["gateway"].state() != MachineGroupState.GROUP_BROKEN && cloud["worker"].state() != MachineGroupState.GROUP_BROKEN && it.machines().size < it.hardMax
        //            HttpUtil.getStatus(URI("http://api.snapito.com/web/monitor/lc/sky.com?freshness=1"), Locale.getDefault(), 60000) >= 400
        //        } after 60 seconds "api-broken"


        snapitoSensorActions(cloud);
    }

    server.start(300)
    try {

        while (true) {
            Thread.sleep(10000)
        }
    } finally  {
        server.stop();
    }

}


