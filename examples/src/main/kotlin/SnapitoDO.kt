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

package kontrol.examples.snapito

import kontrol.digitalocean.DigitalOceanMachineGroup
import kontrol.digitalocean.DigitalOceanClientFactory
import kontrol.sensor.SSHLoadSensor
import kontrol.sensor.HttpStatusSensor
import kontrol.digitalocean.DigitalOceanConfig
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
import kontrol.ext.string.ssh.onHost
import kontrol.postmortem.HeapDumpsPostmortem
import kontrol.common.group.ext.configureStaticActions
import kontrol.impl.sensor.DiskUsageSensor
import kontrol.common.DefaultGroupSensorArray
import kontrol.api.Machine
import kontrol.sensor.RedisListSensor
import kontrol.konfigurators.CloudFlareKonfigurator


val keys = "93676"


public fun snapitoSensorActions(infra: Infrastructure): Infrastructure {
    infra.topology().each { group ->
        when (group.name()) {
            "api-lb", "gateway-lb" -> {
                group.addMachineOverloadRules("load" to 6.0, "http-response-time" to 5000.0)
                group.addGroupSensorRules("http-response-time" to -1.0..2000.0, "load" to 2.0..5.0)
                group.addMachineBrokenRules("http-status" to 399.0)
            }
            "static-linux-snapito.io" -> {
                group.addMachineOverloadRules("load" to 6.0, "http-response-time" to 500.0)
                group.addGroupSensorRules("http-response-time" to -1.0..200.0, "load" to 2.0..5.0)
                group.addMachineBrokenRules("http-status" to 399.0)
            }
            "worker" -> {
                //change this when deployed
                group.addMachineOverloadRules("load" to 3.0, "http-response-time" to 500.0)
                group.addGroupSensorRules("http-response-time" to -1.0..1500.0, "load" to 1.0..3.0, "redis-snapito_snap_queue:linux" to  50.0..500.0)
                group.addMachineBrokenRules("load" to 6.0)
            }
            "api" -> {
                //change this when deployed
                group.addMachineOverloadRules("load" to 3.0, "http-response-time" to 500.0)
                group.addGroupSensorRules("http-response-time" to -1.0..1500.0, "load" to 1.0..3.0)
                group.addMachineBrokenRules("http-status" to 399.0)
            }
            "gateway" -> {
                //change this when deployed
                group.addMachineOverloadRules("load" to 3.0, "http-response-time" to 500.0)
                group.addGroupSensorRules("http-response-time" to -1.0..1500.0, "load" to 1.0..3.0)
                group.addMachineBrokenRules("http-status" to 399.0)
            }
        }
    }
    return infra;
}

fun staticMachines(groupName: String, controller: Controller, sensors: SensorArray, cost: Double, vararg mcs: Pair<String, String>): List<StaticMachine> {
    val list: ArrayList<StaticMachine> = arrayListOf()
    for (mc in mcs) {
        list.add(StaticMachine(sensors, controller, groupName, cost / 24 / 30.5, mc.second, mc.second, mc.second, mc.first))
    }
    return list;

}

fun buildWorkers(client: DigitalOceanClientFactory, controller: Controller, test: Boolean = true, api: MachineGroup): MachineGroup {
    val workerSensorArray = DefaultSensorArray(listOf(SSHLoadSensor(), HttpStatusSensor("/"), DiskUsageSensor()));
    val workerConfig = DigitalOceanConfig(if (test) "dev-snapito-" else "prod-snapito-", "template-snapito-", 4, 62)
    return DigitalOceanMachineGroup(client, controller, "worker", workerSensorArray, workerConfig, keys, 8, 12, 14, arrayListOf<MachineGroup>(api), listOf(CentosPostmortem(), HeapDumpsPostmortem()), groupSensors = DefaultGroupSensorArray(listOf(RedisListSensor("snapito_snap_queue:linux", "redis.snapito.io", 6379, autoTrimAt = 50000))))

}

fun buildAPI(client: DigitalOceanClientFactory, controller: Controller, test: Boolean = true, loadBalancers: MachineGroup): MachineGroup {
    val sensorArray = DefaultSensorArray(listOf(SSHLoadSensor(), HttpStatusSensor("/image?url=google.com&freshness=60"), HttpResponseTimeSensor("/image?url=google.com"), DiskUsageSensor()));
    val doConfig = DigitalOceanConfig(if (test) "dev-snapito-" else "prod-snapito-", "template-snapito-", 4, 62)
    return DigitalOceanMachineGroup(client, controller, "api", sensorArray, doConfig, keys, 2, 4, 8, arrayListOf<MachineGroup>(loadBalancers), listOf(CentosPostmortem(), HeapDumpsPostmortem()), groupSensors = DefaultGroupSensorArray(listOf(RedisListSensor("snapito_snap_queue:linux", "redis.snapito.io", 6379))))

}

fun buildGateway(client: DigitalOceanClientFactory, controller: Controller, test: Boolean = true, loadBalancers: MachineGroup): MachineGroup {
    val sensorArray = DefaultSensorArray(listOf(SSHLoadSensor(), HttpStatusSensor("/gateway?limits"), HttpResponseTimeSensor("/gateway?limits"), DiskUsageSensor()));
    val doConfig = DigitalOceanConfig(if (test) "dev-snapito-" else "prod-snapito-", "template-snapito-", 4, 62)
    return DigitalOceanMachineGroup(client, controller, "gateway", sensorArray, doConfig, keys, 2, 4, 4, arrayListOf<MachineGroup>(loadBalancers), listOf(CentosPostmortem(), HeapDumpsPostmortem()), groupSensors = DefaultGroupSensorArray(listOf(RedisListSensor("snapito_snap_queue:linux", "redis.snapito.io", 6379))))

}

fun buildAPILoadBalancers(client: DigitalOceanClientFactory, controller: Controller, test: Boolean = true): MachineGroup {
    val loadBalancerSensorArray = DefaultSensorArray(listOf(SSHLoadSensor(), HttpStatusSensor("/_stats", 8888), HttpResponseTimeSensor("/_stats", 8888)));
    val lbConfig = DigitalOceanConfig(if (test) "dev-snapito-" else "prod-snapito-", "template-snapito-", 4, 66)
    return DigitalOceanMachineGroup(client, controller, "api-lb", loadBalancerSensorArray, lbConfig, keys, 2, 2, 5, arrayListOf(), listOf(CentosPostmortem()), downStreamKonfigurator = HaproxyKonfigurator("/api-haproxy.cfg.vm"))

}

fun buildGatewayLoadBalancers(client: DigitalOceanClientFactory, controller: Controller, test: Boolean = true): MachineGroup {
    val loadBalancerSensorArray = DefaultSensorArray(listOf(SSHLoadSensor(), HttpStatusSensor("/_stats", 8888), HttpResponseTimeSensor("/_stats", 8888)));
    val lbConfig = DigitalOceanConfig(if (test) "dev-snapito-" else "prod-snapito-", "template-snapito-", 4, 66)
    return DigitalOceanMachineGroup(client, controller, "gateway-lb", loadBalancerSensorArray, lbConfig, keys, 2, 2, 5, arrayListOf(), listOf(CentosPostmortem()), downStreamKonfigurator = HaproxyKonfigurator("/gateway-haproxy.cfg.vm"))

}

fun buildGroups(client: DigitalOceanClientFactory, controller: Controller, test: Boolean = true): Map<String, MachineGroup> {
    val workerSensorArray = DefaultSensorArray(listOf(SSHLoadSensor(), HttpStatusSensor("/"), HttpResponseTimeSensor("/"), DiskUsageSensor()));

    val pinstamaticCloudFlareKonfigurator = CloudFlareKonfigurator(System.getProperty("cf.email") ?: "", System.getProperty("cf.apikey") ?: "", "pinstamatic.com", if (test) "test.pinstamatic.com" else "pinstamatic.com")

    val pinstamaticGroup = DigitalOceanMachineGroup(client, controller, "pinstamatic.com", DefaultSensorArray(listOf(SSHLoadSensor(), HttpStatusSensor("/pinstamatic-api/snap?preview&url=http%3A%2F%2Fpinstamatic.com%2Fcontent-text-sticky.html%3Ftext%3Dtest%26color%3Dyellow"), HttpResponseTimeSensor("/pinstamatic-api/snap?preview&url=http%3A%2F%2Fpinstamatic.com%2Fcontent-text-sticky.html%3Ftext%3Dtest%26color%3Dyellow"))), DigitalOceanConfig(if (test) "test-" else "", "template-snapito-", 4, 62), keys, 2, 2, 4, arrayListOf<MachineGroup>(), listOf(CentosPostmortem(), TomcatPostmortem("/home/cazcade/jetty")), upStreamKonfigurator = pinstamaticCloudFlareKonfigurator)


    val apiLoadBalancers = buildAPILoadBalancers(client, controller, test)
    val gatewayLoadBalancers = buildGatewayLoadBalancers(client, controller, test)
    val api = buildAPI(client, controller, test, apiLoadBalancers)
    return hashMapOf(
            "worker" to buildWorkers(client, controller, test, api),
            "gateway-lb" to gatewayLoadBalancers,
            "gateway" to buildGateway(client, controller, test, gatewayLoadBalancers),
            "api-lb" to apiLoadBalancers,
            "api" to api,
            "pinstamatic" to  pinstamaticGroup

    );
}


fun main(args: Array<String>): Unit {


    val server = Server { controller, bus, postmortems ->
        val cient = DigitalOceanClientFactory(System.getProperty("do.cid") ?: "", System.getProperty("do.apikey") ?: "")
        val cloud = MixedCloud(buildGroups(cient, controller, false))
        cloud.topology().each { group ->
            group.allowDefaultTransitions();
            group.applyDefaultRules(60);
        }
        cloud["pinstamatic"].configureDefaultActions(controller, postmortems);

        val easydeployUpgrade: (Machine, MachineGroup) -> Unit = { m, g -> "/home/easydeploy/bin/update.sh 0s".onHost(m.id(), "root") }
        cloud["worker"].configureDefaultActions(controller, postmortems)
        cloud["api"].configureStaticActions(controller, postmortems)
        cloud["gateway"].configureStaticActions(controller, postmortems)
        cloud["api-lb"].configureStaticActions(controller, postmortems);
        cloud["gateway-lb"].configureStaticActions(controller, postmortems);

        // General rules based on external services

        //        cloud["gateway-lb"] becomes MachineGroupState.GROUP_BROKEN ifStateIn listOf(MachineGroupState.QUIET, MachineGroupState.BUSY, MachineGroupState.NORMAL, null) andTest {
        //            cloud["api-lb"].state() != MachineGroupState.GROUP_BROKEN && cloud["gateway"].state() != MachineGroupState.GROUP_BROKEN &&
        //            HttpUtil.getStatus(URI("http://api.snapito.com/web/monitor/lc/sky.com?freshness=60"), Locale.getDefault(), 60000) >= 400
        //        } after 60 seconds "gateway-lb-broken"
        //
        //        cloud["api-lb"] becomes MachineGroupState.GROUP_BROKEN ifStateIn listOf(MachineGroupState.QUIET, MachineGroupState.BUSY, MachineGroupState.NORMAL, null) andTest {
        //            cloud["api"].state() != MachineGroupState.GROUP_BROKEN && cloud["worker"].state() != MachineGroupState.GROUP_BROKEN &&
        //            HttpUtil.getStatus(URI("http://process.snapito.com/?url=google.com&key=monitor&freshness=60"), Locale.getDefault(), 60000) >= 400
        //        } after 60 seconds "api-lb-broken"


        snapitoSensorActions(cloud);
    }

    server.start(120)
    try {

        while (true) {
            Thread.sleep(10000)
        }
    } finally {
        server.stop();
    }

}


