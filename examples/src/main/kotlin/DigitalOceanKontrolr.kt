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
import kontrol.digitalocean.DigitalOceanCloud
import kontrol.digitalocean.DigitalOceanClientFactory
import kontrol.common.DefaultController
import kontrol.sensor.SSHLoadSensor
import kontrol.sensor.HttpStatusSensor
import kontrol.digitalocean.DigitalOceanConfig
import kontrol.sensor.HttpLoadSensor
import kontrol.common.DefaultSensorArray
import kontrol.api.Infrastructure
import kontrol.api.MachineState.*
import kontrol.common.L
import kontrol.konfigurators.HaproxyKonfigurator
import kontrol.sensor.HttpResponseTimeSensor
import kontrol.status.StatusServer
import kontrol.common.selectStateUsingSensorValues
import kontrol.common.allowDefaultTranstitions
import kontrol.common.applyDefaultPolicies
import kontrol.postmortem.CentosPostmortem
import kontrol.postmortem.JettyPostmortem

/**
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */


public fun snapitoSensorActions(infra: Infrastructure) {
    infra.topology().each { group ->

        group memberIs OK ifStateIn L(BROKEN, STARTING) andTest { it["http-status"]?.I()?:999 < 400 && it["load"]?.D()?:0.0 < 30 } after 5 checks "http-ok"
        group memberIs DEAD ifStateIn L(STOPPED) after 50 checks "stopped-now-dead"
        group memberIs BROKEN ifStateIn L(OK, STALE, STARTING) andTest { it["load"]?.D()?:0.0 > 30 } after 20 checks "mega-overload"
        group memberIs DEAD ifStateIn L(BROKEN) andTest { it["http-status"]?.I()?:0 > 400 } after 80 checks "broken-now-dead"


        when(group.name()) {
            "lb", "gateway" -> {

                group memberIs BROKEN ifStateIn L(OK, STALE, STARTING) andTest {
                    it["http-status"]?.I()?:999 >= 400
                } after 20 checks "http-broken"

                group.selectStateUsingSensorValues("load" to 1.0..5.0)
            }

            "worker" -> {

                group memberIs BROKEN ifStateIn L(OK, STALE, STARTING) andTest {
                    it["http-status"]?.I()?:999 >= 400 && it["http-load"]?.D()?:2.0 < 30.0
                } after 30 checks "http-broken"

                group memberIs BROKEN ifStateIn L(OK, STALE, STARTING) andTest { it["http-response-time"]?.I()?:0 > 2000 } after 50 checks "response-too-long"
                group.selectStateUsingSensorValues("http-response-time" to -1.0..1000.0, "http-load" to 4.0..8.0)

            }
        }
    }
}


fun buildToplogy(client: DigitalOceanClientFactory, test: Boolean = true): Map<String, DigitalOceanMachineGroup> {
    val gatewaySensorArray = DefaultSensorArray<Any?>(listOf(SSHLoadSensor(), HttpStatusSensor("/gateway?status")));
    val loadBalancerSensorArray = DefaultSensorArray<Any?>(listOf(SSHLoadSensor(), HttpStatusSensor("/gateway?status")));
    val workerSensorArray = DefaultSensorArray<Any?>(listOf(SSHLoadSensor(), HttpLoadSensor("/api/load"), HttpStatusSensor("/api?url=google.com"), HttpResponseTimeSensor("/api?url=google.com&freshness=1")));

    val lbConfig = DigitalOceanConfig(if (test) "test-snapito-" else "prod-snapito-", "template-snapito-", 4, 66)
    val gatewayConfig = DigitalOceanConfig(if (test) "test-snapito-" else "prod-snapito-", "template-snapito-", 4, 62)
    val workerConfig = DigitalOceanConfig(if (test) "test-snapito-" else "prod-snapito-", "template-snapito-", 4, 65)

    val keys = "Neil Laptop,Eric New"

    val cloudFlareKonfigurator = CloudFlareKonfigurator(System.getProperty("cf.email")?:"", System.getProperty("cf.apikey")?:"", "snapito.com", if (test) "test.api" else "api")

    val loadBalancerGroup = DigitalOceanMachineGroup(client, "lb", loadBalancerSensorArray, lbConfig, keys, 2, 2, listOf(), listOf(CentosPostmortem()), downStreamKonfigurator = HaproxyKonfigurator("/haproxy.cfg.vm"), upStreamKonfigurator = cloudFlareKonfigurator
    )

    val gatewayGroup = DigitalOceanMachineGroup(client, "gateway", gatewaySensorArray, gatewayConfig, keys, 2, 2, listOf(loadBalancerGroup), listOf(CentosPostmortem(), JettyPostmortem("/home/cazcade/jetty")))

    val workerGroup = DigitalOceanMachineGroup(client, "worker", workerSensorArray, workerConfig, keys, 3, 20, listOf(gatewayGroup), listOf(CentosPostmortem(), JettyPostmortem("/home/cazcade/jetty")), upStreamKonfigurator = WorkerKonfigurator())


    return hashMapOf(
            "worker" to workerGroup,
            "gateway" to gatewayGroup,
            "lb" to loadBalancerGroup
    );
}


fun main(args: Array<String>): Unit {

    val digitalOceanClient = DigitalOceanClientFactory(System.getProperty("do.cid")?:"", System.getProperty("do.apikey")?:"")
    val cloud = DigitalOceanCloud(digitalOceanClient, buildToplogy(digitalOceanClient, false))
    val bus = NullBus()
    val controller = DefaultController(bus)
    val statusServer = StatusServer(cloud, bus)

    snapitoSensorActions(cloud);
    cloud.topology().each { group -> group.allowDefaultTranstitions(); group.applyDefaultPolicies(controller, { println(it.toString());java.lang.String(it.toString()) }) }

    cloud.start()
    statusServer.start()

    while (true) {
        Thread.sleep(10000)
        //            println(cloud.topology().toString());
    }
    //        cloud.stop();
}


