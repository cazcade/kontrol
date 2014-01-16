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
import kontrol.api.MachineGroupState.*
import kontrol.api.Action.*
import kontrol.api.GroupAction.*
import kontrol.api.Controller
import kontrol.api.MachineGroup
import kontrol.api.MachineGroup.Recheck.*;
import kontrol.common.L
import kontrol.konfigurators.HaproxyKonfigurator
import kontrol.sensor.HttpResponseTimeSensor

/**
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */


public fun defaultTranstitions(group: MachineGroup) {

    group allowMachine (STARTING to OK);
    group allowMachine (STARTING to BROKEN);
    group allowMachine (STARTING to STOPPED);
    group allowMachine (OK to STOPPING);
    group allowMachine (OK to STOPPED);
    group allowMachine (OK to BROKEN);
    group allowMachine (OK to STALE);
    group allowMachine (STOPPING to STOPPED);
    group allowMachine (STOPPING to STARTING);
    group allowMachine (STOPPED to DEAD);
    group allowMachine (BROKEN to STOPPING);
    group allowMachine (BROKEN to STOPPED);
    group allowMachine (BROKEN to OK);
    group allowMachine (BROKEN to DEAD);
    group allowMachine (STALE to  STOPPING);

    group allow (QUIET to BUSY);
    group allow (QUIET to NORMAL);
    group allow (QUIET to GROUP_BROKEN);
    group allow (BUSY to NORMAL);
    group allow (BUSY to QUIET);
    group allow (BUSY to  GROUP_BROKEN);
    group allow (NORMAL to GROUP_BROKEN);
    group allow (NORMAL to QUIET);
    group allow (NORMAL to BUSY);
    group allow (GROUP_BROKEN to QUIET);
    group allow (GROUP_BROKEN to BUSY);
    group allow (GROUP_BROKEN to NORMAL);
}


public fun snapitoSensorActions(infra: Infrastructure) {
    infra.topology().each {
        val GROUP = it;

        GROUP MACHINE_IS OK IF L(BROKEN, STARTING) AND { it["http-status"]?.I()?:999 < 400 && it["load"]?.D()?:0.0 < 30 } AFTER 5 CHECKS "http-ok"
        GROUP MACHINE_IS DEAD IF L(STOPPED) AFTER 50 CHECKS "stopped-now-dead"


        when(it.name()) {
            "lb" -> {
                val BALANCER = it;
                BALANCER MACHINE_IS BROKEN IF L(OK, STALE, STARTING) AND { it["http-status"]?.I()?:999 >= 400 } AFTER 20 CHECKS "http-broken"
                BALANCER MACHINE_IS BROKEN IF L(OK, STALE, STARTING) AND { it["load"]?.D()?:0.0 > 30 } AFTER 2 CHECKS "mega-overload"
                BALANCER MACHINE_IS DEAD IF L(BROKEN) AND { it["http-status"]?.I()?:0 > 400 } AFTER 20 CHECKS "broken-now-dead"

                BALANCER IS BUSY IF L(QUIET, BUSY, NORMAL, null) AND { it["load"]?:0.0 > 5.0 || BALANCER.workingSize() < BALANCER.min }  AFTER 10 CHECKS "overload"
                BALANCER IS QUIET IF L(QUIET, BUSY, NORMAL, null) AND { it["load"]?:8.0 < 1.0 || BALANCER.activeSize() > BALANCER.max }  AFTER 20 CHECKS "underload"
                BALANCER IS NORMAL IF L(QUIET, BUSY, null) AND { it["load"]?:1.0 in 1.0..5.0 && BALANCER.activeSize() in BALANCER.min..BALANCER.max }  AFTER 5 CHECKS "group-ok"

            }
            "gateway" -> {
                val GATEWAY = it;
                GATEWAY MACHINE_IS BROKEN IF L(OK, STALE, STARTING) AND { it["http-status"]?.I()?:999 >= 400 } AFTER 20 CHECKS "http-broken"
                GATEWAY MACHINE_IS BROKEN IF L(OK, STALE, STARTING) AND { it["load"]?.D()?:0.0 > 30 } AFTER 3 CHECKS "mega-overload"
                GATEWAY MACHINE_IS DEAD IF L(BROKEN) AND { it["http-status"]?.I()?:0 > 400 } AFTER 20 CHECKS "broken-now-dead"

                GATEWAY IS BUSY IF L(QUIET, BUSY, NORMAL, null) AND { it["load"]?:0.0 > 5.0 || GATEWAY.workingSize() < GATEWAY.min }  AFTER 10 CHECKS "overload"
                GATEWAY IS QUIET IF L(QUIET, BUSY, NORMAL, null) AND { it["load"]?:5.0 < 2.0 || GATEWAY.activeSize() > GATEWAY.max }  AFTER 20 CHECKS "underload"
                GATEWAY IS NORMAL IF L(QUIET, BUSY, null) AND { it["load"]?:0.0 in 2.0..5.0 && GATEWAY.activeSize() in GATEWAY.min..GATEWAY.max }  AFTER 5 CHECKS "group-ok"

            }
            "worker" -> {
                val WORKER = it;
                WORKER MACHINE_IS BROKEN IF L(OK, STALE, STARTING) AND { it["http-status"]?.I()?:999 >= 400 && it["http-load"]?.D()?:2.0 < 2.0 } AFTER 30 CHECKS "http-broken"
                WORKER MACHINE_IS BROKEN IF L(OK, STALE, STARTING) AND { it["load"]?.D()?:0.0 > 30 } AFTER 5 CHECKS "mega-overload"
                WORKER MACHINE_IS DEAD IF L(BROKEN) AND { it["http-status"]?.I()?:0 > 400 } AFTER 100 CHECKS "broken-now-dead"

                WORKER IS BUSY IF L(QUIET, BUSY, NORMAL, null) AND { it["http-response-time"]?:0.0 > 2000 || it["http-load"]?:0.0 > 8.0 || WORKER.workingSize() < WORKER.min }  AFTER 20 CHECKS "overload"
                WORKER IS QUIET IF L(QUIET, BUSY, NORMAL, null) AND { it["http-load"]?:8.0 < 5.0 || WORKER.activeSize() > WORKER.max }  AFTER 120 CHECKS "underload"
                WORKER IS NORMAL IF L(QUIET, BUSY, null) AND { it["http-load"]?:1.0 in 5.0..8.0 && WORKER.activeSize() in WORKER.min..WORKER.max }  AFTER 5 CHECKS "group-ok"
            }
        }
    }
}

public fun snapitoPolicy(infra: Infrastructure, controller: Controller) {
    infra.topology().each {
        defaultTranstitions(it);
        val group = it
        group MACHINE BROKEN RECHECK THEN TELL controller  TO RESTART_MACHINE;
        group MACHINE DEAD RECHECK THEN TELL controller  TO REIMAGE_MACHINE ;
        group MACHINE STALE RECHECK THEN TELL controller   TO REIMAGE_MACHINE;
        group BECOME BUSY RECHECK THEN USE controller TO EXPAND;
        group BECOME QUIET RECHECK THEN USE controller  TO CONTRACT;
        when(it.name()) {
            "lb" -> {
                val balancers = it;
            }
            "gateway" -> {
                val gateways = it;
            }
            "worker" -> {
                val workers = it;
            }
        }
    }
}

public fun snapitoStrategy(infra: Infrastructure, controller: Controller) {
    infra.topology().each {
        val group = it;
        controller USE { group.failAction(it) { group.reImage(it) } }TO REIMAGE_MACHINE IN_GROUP group;
        controller USE { group.failAction(it) { group.restart(it) } } TO RESTART_MACHINE IN_GROUP group;
        controller WILL { group.expand() } TO EXPAND  UNLESS { group.activeSize() >= group.max }  GROUP group;
        controller WILL { group.contract() } TO CONTRACT UNLESS { group.activeSize() <= group.min } GROUP group;
        when(it.name()) {
            "lb" -> {
                val balancers = it;
            }
            "gateway" -> {
                val gateways = it;

            }
            "worker" -> {
                val workers = it;
                controller USE { workers.failover(it).destroy(it) } TO DESTROY_MACHINE IN_GROUP workers;
            }
        }
    };
}

fun buildToplogy(client: DigitalOceanClientFactory, test: Boolean = true): Map<String, DigitalOceanMachineGroup> {
    val gatewaySensorArray = DefaultSensorArray<Any?>(listOf(SSHLoadSensor(), HttpStatusSensor("/gateway?status")));
    val loadBalancerSensorArray = DefaultSensorArray<Any?>(listOf(SSHLoadSensor(), HttpStatusSensor("/")));
    val workerSensorArray = DefaultSensorArray<Any?>(listOf(SSHLoadSensor(), HttpLoadSensor("/api/load"), HttpStatusSensor("/api?url=google.com"), HttpResponseTimeSensor("/api?url=google.com")));
    val lbConfig = DigitalOceanConfig(if (test) "test-snapito-" else "prod-snapito-", "template-snapito-", 4, 66)
    val gatewayConfig = DigitalOceanConfig(if (test) "test-snapito-" else "prod-snapito-", "template-snapito-", 4, 62)
    val workerConfig = DigitalOceanConfig(if (test) "test-snapito-" else "prod-snapito-", "template-snapito-", 4, 65)

    val loadBalancerGroup = DigitalOceanMachineGroup(client, "lb", loadBalancerSensorArray, lbConfig, "Neil Laptop,Eric New", 2, 2, listOf(), downStreamKonfigurator = HaproxyKonfigurator("/haproxy.cfg.vm"), upStreamKonfigurator = CloudFlareKonfigurator(System.getProperty("cf.email")?:"", System.getProperty("cf.apikey")?:"", "snapito.com", if (test) "test.api" else "api"))
    val gatewayGroup = DigitalOceanMachineGroup(client, "gateway", gatewaySensorArray, gatewayConfig, "Neil Laptop,Eric New", 2, 2, listOf(loadBalancerGroup))
    val workerGroup = DigitalOceanMachineGroup(client, "worker", workerSensorArray, workerConfig, "Neil Laptop,Eric New", 3, 20, listOf(gatewayGroup))
    val map: MutableMap<String, DigitalOceanMachineGroup> = hashMapOf(
            "worker" to workerGroup,
            "gateway" to gatewayGroup,
            "lb" to loadBalancerGroup
    );
    return map;
}


fun main(args: Array<String>): Unit {

    val digitalOceanClient = DigitalOceanClientFactory(System.getProperty("do.cid")?:"", System.getProperty("do.apikey")?:"")
    val groups = buildToplogy(digitalOceanClient, false);
    val cloud = DigitalOceanCloud(digitalOceanClient, groups);
    val controller = DefaultController();

    snapitoSensorActions(cloud);
    snapitoPolicy(cloud, controller);
    snapitoStrategy(cloud, controller);
    cloud.start();
    while (true) {
        Thread.sleep(10000)
        //            println(cloud.topology().toString());
    }
    //        cloud.stop();
}


