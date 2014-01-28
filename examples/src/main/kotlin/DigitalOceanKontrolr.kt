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
import kontrol.sensor.SSHLoadSensor
import kontrol.sensor.HttpStatusSensor
import kontrol.digitalocean.DigitalOceanConfig
import kontrol.sensor.HttpLoadSensor
import kontrol.common.DefaultSensorArray
import kontrol.api.Infrastructure
import kontrol.api.MachineState.*
import kontrol.konfigurators.HaproxyKonfigurator
import kontrol.sensor.HttpResponseTimeSensor
import kontrol.postmortem.CentosPostmortem
import kontrol.postmortem.JettyPostmortem
import kontrol.server.Server
import kontrol.api.Controller
import kontrol.common.group.ext.addSensorRules
import kontrol.common.group.ext.allowDefaultTransitions
import kontrol.common.group.ext.applyDefaultRules
import kontrol.common.group.ext.applyDefaultPolicies

/**
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */


public fun snapitoSensorActions(infra: Infrastructure): Infrastructure {
    infra.topology().each { group ->

        group memberIs OK ifStateIn listOf(BROKEN, STARTING) andTest { it["http-status"]?.I()?:999 < 400 && it["load"]?.D()?:0.0 < 30 } after 30 seconds "http-ok"
        group memberIs BROKEN ifStateIn listOf(OK, STALE) andTest { it["load"]?.D()?:0.0 > 30 } after 240 seconds "mega-overload"
        group memberIs BROKEN ifStateIn listOf(OK, STALE, STARTING) andTest { it["http-status"]?.I()?:999 >= 400 } after 300 seconds "http-broken"
        group memberIs BROKEN ifStateIn listOf(OK, STALE, STARTING) andTest { it["http-response-time"]?.I()?:9000 > 3000 } after 240 seconds "response-too-long"

        when(group.name()) {
            "lb", "gateway" -> {
                group.addSensorRules("http-response-time" to -1.0..2000.0, "load" to 1.0..5.0)
            }
            "worker" -> {
                //change this when deployed
                group.addSensorRules("http-response-time" to -1.0..2000.0, "http-load" to 2.0..8.0)
            }
        }
    }
    return infra;
}


fun buildGroups(client: DigitalOceanClientFactory, controller: Controller, test: Boolean = true): Map<String, DigitalOceanMachineGroup> {
    val cloudFlareKonfigurator = CloudFlareKonfigurator(System.getProperty("cf.email")?:"", System.getProperty("cf.apikey")?:"", "snapito.com", if (test) "test.api" else "api")

    val gatewaySensorArray = DefaultSensorArray(listOf(SSHLoadSensor(), HttpStatusSensor("/gateway?status"), HttpResponseTimeSensor("/gateway?status")));
    val loadBalancerSensorArray = DefaultSensorArray(listOf(SSHLoadSensor(), HttpStatusSensor("/gateway?status"), HttpResponseTimeSensor("/gateway?status")));
    val workerSensorArray = DefaultSensorArray(listOf(SSHLoadSensor(), HttpLoadSensor("/api/load"), HttpStatusSensor("/api?url=google.com"), HttpResponseTimeSensor("/api?url=google.com&freshness=1")));

    val lbConfig = DigitalOceanConfig(if (test) "test-snapito-" else "prod-snapito-", "template-snapito-", 4, 66)
    val gatewayConfig = DigitalOceanConfig(if (test) "test-snapito-" else "prod-snapito-", "template-snapito-", 4, 62)
    val workerConfig = DigitalOceanConfig(if (test) "test-snapito-" else "prod-snapito-", "template-snapito-", 4, 65)

    val keys = "Neil Laptop,Eric New"


    val loadBalancerGroup = DigitalOceanMachineGroup(client, controller, "lb", loadBalancerSensorArray, lbConfig, keys, 2, 2, listOf(), listOf(CentosPostmortem()), downStreamKonfigurator = HaproxyKonfigurator("/haproxy.cfg.vm"), upStreamKonfigurator = cloudFlareKonfigurator
    )

    val gatewayGroup = DigitalOceanMachineGroup(client, controller, "gateway", gatewaySensorArray, gatewayConfig, keys, 2, 2, listOf(loadBalancerGroup), listOf(CentosPostmortem(), JettyPostmortem("/home/cazcade/jetty")))

    val workerGroup = DigitalOceanMachineGroup(client, controller, "worker", workerSensorArray, workerConfig, keys, 3, 8, listOf(gatewayGroup), listOf(CentosPostmortem(), JettyPostmortem("/home/cazcade/jetty")), upStreamKonfigurator = WorkerKonfigurator())


    return hashMapOf(
            "worker" to workerGroup,
            "gateway" to gatewayGroup,
            "lb" to loadBalancerGroup
    );
}


fun main(args: Array<String>): Unit {


    val server = Server { controller, bus, postmortems ->
        val cient = DigitalOceanClientFactory(System.getProperty("do.cid")?:"", System.getProperty("do.apikey")?:"")
        val cloud = DigitalOceanCloud(cient, buildGroups(cient, controller, false))
        cloud.topology().each { group ->
            group.allowDefaultTransitions();
            group.applyDefaultPolicies(controller, postmortems)
            group.applyDefaultRules();
        }
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


