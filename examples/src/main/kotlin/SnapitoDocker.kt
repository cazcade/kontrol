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
import kontrol.common.group.ext.configureDefaultActions
import kontrol.common.group.ext.applyDefaultRules
import kontrol.common.group.ext.addMachineBrokenRules
import kontrol.konfigurators.HaproxyKonfigurator
import kontrol.staticmc.StaticMachineGroup
import kontrol.ext.string.ssh.onHost
import kontrol.api.OS
import kontrol.postmortem.HeapDumpsPostmortem
import kontrol.common.group.ext.configureStaticActions
import kontrol.impl.sensor.DiskUsageSensor
import kontrol.common.DefaultGroupSensorArray
import kontrol.api.Machine


val keys = "93676"


public fun snapitoSensorActions(infra: Infrastructure): Infrastructure {
    infra.topology().each { group ->
        group.addMachineBrokenRules("http-status" to 399.0)
        when(group.name()) {
            "api-lb", "gateway-lb" -> {
                group.addMachineOverloadRules("load" to 6.0, "http-response-time" to 5000.0)
                group.addGroupSensorRules("http-response-time" to -1.0..2000.0, "load" to 2.0..5.0)
            }
            "static-linux-snapito.io" -> {
                group.addMachineOverloadRules("load" to 6.0, "http-response-time" to 500.0)
                group.addGroupSensorRules("http-response-time" to -1.0..200.0, "load" to 2.0..5.0)
            }
            "dockworker" -> {
                //change this when deployed
                group.addMachineOverloadRules("load" to 3.0, "http-response-time" to 500.0)
                group.addGroupSensorRules("http-response-time" to -1.0..1500.0, "load" to 1.0..3.0, "redis-snapito_snap_queue:linux" to  50.0..500.0)
            }
            "api" -> {
                //change this when deployed
                group.addMachineOverloadRules("load" to 3.0, "http-response-time" to 500.0)
                group.addGroupSensorRules("http-response-time" to -1.0..1500.0, "load" to 1.0..3.0)
            }
            "gateway" -> {
                //change this when deployed
                group.addMachineOverloadRules("load" to 3.0, "http-response-time" to 500.0)
                group.addGroupSensorRules("http-response-time" to -1.0..1500.0, "load" to 1.0..3.0)
            }
            "static-linux-workers", "pinstamatic" -> {
                //change this when deployed
                group.addMachineOverloadRules("http-load" to 50.0, "load" to 3.0, "http-response-time" to 500.0)
                group.addGroupSensorRules("http-response-time" to -1.0..1500.0, "http-load" to 1.0..3.0)
            }
            "static-osx-workers" -> {
                //change this when deployed
                group.addMachineOverloadRules("load" to 10.0, "http-response-time" to 3000.0)
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

fun buildDockerWorkers(client: DigitalOceanClientFactory, controller: Controller, test: Boolean = true): MachineGroup {
    val workerSensorArray = DefaultSensorArray(listOf(SSHLoadSensor(), HttpStatusSensor("/"), HttpResponseTimeSensor("/"), DiskUsageSensor()));
    val workerConfig = DigitalOceanConfig(if (test) "test-snapito-" else "prod-snapito-", "template-snapito-", 4, 62)
    return  DigitalOceanMachineGroup(client, controller, "dockworker", workerSensorArray, workerConfig, keys, 0, 12, 16, arrayListOf<MachineGroup>(), listOf(CentosPostmortem(), HeapDumpsPostmortem()), groupSensors = DefaultGroupSensorArray(listOf(RedisListSensor("snapito_snap_queue:linux", "107.170.24.38", 6379, autoTrimAt = 50000))))

}

fun buildAPI(client: DigitalOceanClientFactory, controller: Controller, test: Boolean = true, loadBalancers: MachineGroup): MachineGroup {
    val sensorArray = DefaultSensorArray(listOf(SSHLoadSensor(), HttpStatusSensor("/image?url=google.com&freshness=60"), HttpResponseTimeSensor("/image?url=google.com"), DiskUsageSensor()));
    val doConfig = DigitalOceanConfig(if (test) "test-snapito-" else "prod-snapito-", "template-snapito-", 4, 62)
    return DigitalOceanMachineGroup(client, controller, "api", sensorArray, doConfig, keys, 2, 4, 4, arrayListOf<MachineGroup>(loadBalancers), listOf(CentosPostmortem(), HeapDumpsPostmortem()), groupSensors = DefaultGroupSensorArray(listOf(RedisListSensor("snapito_snap_queue:linux", "107.170.24.38", 6379))))

}

fun buildGateway(client: DigitalOceanClientFactory, controller: Controller, test: Boolean = true, loadBalancers: MachineGroup): MachineGroup {
    val sensorArray = DefaultSensorArray(listOf(SSHLoadSensor(), HttpStatusSensor("/gateway?limits"), HttpResponseTimeSensor("/gateway?limits"), DiskUsageSensor()));
    val doConfig = DigitalOceanConfig(if (test) "test-snapito-" else "prod-snapito-", "template-snapito-", 4, 62)
    return DigitalOceanMachineGroup(client, controller, "gateway", sensorArray, doConfig, keys, 2, 4, 4, arrayListOf<MachineGroup>(loadBalancers), listOf(CentosPostmortem(), HeapDumpsPostmortem()), groupSensors = DefaultGroupSensorArray(listOf(RedisListSensor("snapito_snap_queue:linux", "107.170.24.38", 6379))))

}

fun buildAPILoadBalancers(client: DigitalOceanClientFactory, controller: Controller, test: Boolean = true): MachineGroup {
    val loadBalancerSensorArray = DefaultSensorArray(listOf(SSHLoadSensor(), HttpStatusSensor("/load"), HttpResponseTimeSensor("/_stats", 8888)));
    val lbConfig = DigitalOceanConfig(if (test) "test-snapito-" else "prod-snapito-", "template-snapito-", 4, 66)
    return DigitalOceanMachineGroup(client, controller, "api-lb", loadBalancerSensorArray, lbConfig, keys, 2, 2, 5, arrayListOf(), listOf(CentosPostmortem()), downStreamKonfigurator = HaproxyKonfigurator("/api-haproxy.cfg.vm"))

}

fun buildGatewayLoadBalancers(client: DigitalOceanClientFactory, controller: Controller, test: Boolean = true): MachineGroup {
    val loadBalancerSensorArray = DefaultSensorArray(listOf(SSHLoadSensor(), HttpStatusSensor("/gateway?gatewayVersion"), HttpResponseTimeSensor("/_stats", 8888)));
    val lbConfig = DigitalOceanConfig(if (test) "test-snapito-" else "prod-snapito-", "template-snapito-", 4, 66)
    return DigitalOceanMachineGroup(client, controller, "gateway-lb", loadBalancerSensorArray, lbConfig, keys, 2, 2, 5, arrayListOf(), listOf(CentosPostmortem()), downStreamKonfigurator = HaproxyKonfigurator("/gateway-haproxy.cfg.vm"))

}

fun buildGroups(client: DigitalOceanClientFactory, controller: Controller, test: Boolean = true): Map<String, MachineGroup> {
    val cloudFlareKonfigurator = CloudFlareKonfigurator(System.getProperty("cf.email")?:"", System.getProperty("cf.apikey")?:"", "snapito.com", if (test) "test.api" else "api")
    val workerSensorArray = DefaultSensorArray(listOf(SSHLoadSensor(), HttpStatusSensor("/"), HttpResponseTimeSensor("/"), DiskUsageSensor()));
    val staticWorkerSensorArray = DefaultSensorArray(listOf(SSHLoadSensor(), DiskUsageSensor(), HttpStatusSensor("/")));
    val staticOSXWorkerSensorArray = DefaultSensorArray(listOf(SSHLoadSensor("administrator", OS.OSX), HttpLoadSensor("/api/load"), HttpStatusSensor("/api?url=google.com&freshness=60&key=monitor"), HttpResponseTimeSensor("/api?url=google.com&freshness=1&key=monitor")));

    val staticOSXWorkers = StaticMachineGroup(staticMachines("static-osx-workers", controller, staticOSXWorkerSensorArray, 109.0, "osx-worker-1" to "208.52.187.175", "osx-worker-2" to "208.52.187.176", "osx-worker-3" to "208.52.187.180", "osx-worker-4" to "208.52.187.178", "osx-worker-5" to "208.52.187.179"), " sudo reboot ", "rm -rf ~/cazcade/images/*; sudo reboot", "administrator", controller, "static-osx-workers", staticOSXWorkerSensorArray, keys, arrayListOf(), arrayListOf(TomcatPostmortem("/usr/local/tomcat"), HeapDumpsPostmortem()), upStreamKonfigurator = WorkerKonfigurator("administrator"))

    val contaboMachines = staticMachines("static-linux-workers", controller, workerSensorArray, 149.0, "contabo-worker-1" to "80.241.209.220")
    val hetznerMachines = staticMachines("static-linux-workers", controller, workerSensorArray, 149.0, "EOL!!!-hetzner-worker-1" to "144.76.194.178")
    val onlineNetMachines = staticMachines("static-linux-workers", controller, workerSensorArray, 80.0, "online.net-1" to "62.210.188.41", "online.net-2" to "62.210.188.85", "online.net-2" to  "62.210.146.72")
    val staticLinuxMachines = contaboMachines plus hetznerMachines plus onlineNetMachines;

    val staticLinuxWorkers = StaticMachineGroup(staticLinuxMachines, "sudo docker rm $(docker ps -q -a) ; sudo service supervisor restart", "sudo find /usr/share/nginx/html/temp_images -exec rm {} ; sudo reboot", "cazcade", controller, "static-linux-workers", staticWorkerSensorArray, keys, arrayListOf(), arrayListOf(CentosPostmortem()))


    val snapitoIOSensorArray = DefaultSensorArray(listOf(SSHLoadSensor(), HttpStatusSensor("/snapito.txt"), HttpResponseTimeSensor("/snapito.txt")));
    val snapitoIOMachines = staticMachines("static-linux-snapito.io", controller, snapitoIOSensorArray, 80.0, "snapito.io-new-1" to "62.210.206.130")
    val snapitoIO = StaticMachineGroup(snapitoIOMachines, "reboot", "reboot", "root", controller, "static-linux-snapito.io", snapitoIOSensorArray, keys, arrayListOf(), arrayListOf(CentosPostmortem()));


    val pinstamaticCloudFlareKonfigurator = CloudFlareKonfigurator(System.getProperty("cf.email")?:"", System.getProperty("cf.apikey")?:"", "pinstamatic.com", if (test) "test.pinstamatic.com" else "pinstamatic.com")

    val pinstamaticGroup = DigitalOceanMachineGroup(client, controller, "pinstamatic.com", DefaultSensorArray(listOf(SSHLoadSensor(), HttpStatusSensor("/pinstamatic-api/snap?preview&url=http%3A%2F%2Fpinstamatic.com%2Fcontent-text-sticky.html%3Ftext%3Dtest%26color%3Dyellow"), HttpResponseTimeSensor("/pinstamatic-api/snap?preview&url=http%3A%2F%2Fpinstamatic.com%2Fcontent-text-sticky.html%3Ftext%3Dtest%26color%3Dyellow"))), DigitalOceanConfig(if (test) "test-" else "", "template-snapito-", 4, 62), keys, 2, 2, 4, arrayListOf<MachineGroup>(), listOf(CentosPostmortem(), TomcatPostmortem("/home/cazcade/jetty")), upStreamKonfigurator = pinstamaticCloudFlareKonfigurator)


    val apiLoadBalancers = buildAPILoadBalancers(client, controller, test)
    val gatewayLoadBalancers = buildGatewayLoadBalancers(client, controller, test)

    return hashMapOf(
            "dockworker" to buildDockerWorkers(client, controller, test),
            "gateway-lb" to gatewayLoadBalancers,
            "gateway" to buildGateway(client, controller, test, gatewayLoadBalancers),
            "static-osx-workers" to staticOSXWorkers,
            "static-linux-workers" to staticLinuxWorkers,
            "static-linux-snapito.io" to snapitoIO,
            "api-lb" to apiLoadBalancers,
            "api" to buildAPI(client, controller, test, apiLoadBalancers),
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
        cloud["pinstamatic"].configureDefaultActions(controller, postmortems);
        //        cloud["gateway"].applyDefaultPolicies(controller, postmortems, { m, g ->
        //            " su  cazcade -c 'cd ~/snapito; git checkout prod; git pull; mvn clean install -Dmaven.test.skip=true; killall java; rm -rf /tmp/*.png;sleep 30; ~/jetty/bin/jetty.sh start;sleep 180'".onHost(m.ip())
        //        }, { m, g ->
        //            "su cazcade -c 'cd ~/snapito; git checkout master; git pull; mvn clean install -Dmaven.test.skip=true; killall java; rm -rf /tmp/*.png; ~/jetty/bin/jetty.sh restart'".onHost(m.ip())
        //        })
        //        cloud["lb"].applyDefaultPolicies(controller, postmortems, { m, g -> g.configure(m) })



        cloud["static-osx-workers"].configureStaticActions(controller, postmortems, { m, g ->
            " cd app/snapito; git checkout master; git pull; mvn clean install -Dmaven.test.skip=true; killall java; cp -f ./snapito-api/target/snapito-api-1.0-SNAPSHOT.war /usr/local/tomcat/webapps/api.war; rm -rf /tmp/*.png; sleep 180".onHost(m.ip(), "administrator", timeoutInSeconds = 300)
        },
                { m, g ->
                    " cd app/snapito; git checkout master; git pull; mvn clean install -Dmaven.test.skip=true; killall java;cp -f ./snapito-api/target/snapito-api-1.0-SNAPSHOT.war /usr/local/tomcat/webapps/api.war; rm -rf /tmp/*.png; sleep 120".onHost(m.ip(), "administrator", timeoutInSeconds = 300)

                }
        )
        val easydeployUpgrade: (Machine, MachineGroup) -> Unit = { m, g -> "/home/easydeploy/bin/update.sh 0s".onHost(m.id(), "root") }
        cloud["static-linux-snapito.io"].configureStaticActions(controller, postmortems);
        cloud["static-linux-workers"].configureStaticActions(controller, postmortems);
        cloud["dockworker"].configureDefaultActions(controller, postmortems)
        cloud["api"].configureDefaultActions(controller, postmortems)
        cloud["gateway"].configureDefaultActions(controller, postmortems, easydeployUpgrade)
        cloud["api-lb"].configureStaticActions(controller, postmortems);
        cloud["gateway-lb"].configureStaticActions(controller, postmortems);

        //        cloud["lb"] becomes MachineGroupState.GROUP_BROKEN ifStateIn listOf(MachineGroupState.QUIET, MachineGroupState.BUSY, MachineGroupState.NORMAL, null) andTest {
        //            cloud["gateway"].state() != MachineGroupState.GROUP_BROKEN && cloud["worker"].state() != MachineGroupState.GROUP_BROKEN && it.machines().size < it.hardMax
        //            HttpUtil.getStatus(URI("http://api.snapito.com/web/monitor/lc/sky.com?freshness=1"), Locale.getDefault(), 60000) >= 400
        //        } after 60 seconds "api-broken"


        snapitoSensorActions(cloud);
    }

    server.start(120)
    try {

        while (true) {
            Thread.sleep(10000)
        }
    } finally  {
        server.stop();
    }

}


