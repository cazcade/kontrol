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

        GROUP IS BUSY IF L(QUIET, BUSY, NORMAL, null) AND { it["http-load"]?:0.0 > 6.0 || GROUP.size() < GROUP.min }  AFTER 60 CHECKS "overload"
        GROUP IS QUIET IF L(QUIET, BUSY, NORMAL, null) AND { it["http-load"]?:6.0 < 3.0 || GROUP.size() > GROUP.max }  AFTER 120 CHECKS "underload"
        GROUP IS NORMAL IF L(QUIET, BUSY, null) AND { it["http-load"]?:1.0 in 3.0..6.0 && GROUP.size() in GROUP.min..GROUP.max }  AFTER 5 CHECKS "group-ok"

        when(it.name()) {
            "lb" -> {
                val BALANCER = it;
                BALANCER MACHINE_IS BROKEN IF L(OK, STALE, STARTING) AND { it["http-status"]?.I()?:999 >= 400 } AFTER 5 CHECKS "http-broken"
                BALANCER MACHINE_IS BROKEN IF L(OK, STALE, STARTING) AND { it["load"]?.D()?:0.0 > 30 } AFTER 2 CHECKS "mega-overload"
                BALANCER MACHINE_IS DEAD IF L(BROKEN) AND { it["http-status"]?.I()?:0 > 400 } AFTER 20 CHECKS "broken-now-dead"

            }
            "gateway" -> {
                val GATEWAY = it;
                GATEWAY MACHINE_IS BROKEN IF L(OK, STALE, STARTING) AND { it["http-status"]?.I()?:999 >= 400 } AFTER 3 CHECKS "http-broken"
                GATEWAY MACHINE_IS BROKEN IF L(OK, STALE, STARTING) AND { it["load"]?.D()?:0.0 > 30 } AFTER 3 CHECKS "mega-overload"
                GATEWAY MACHINE_IS DEAD IF L(BROKEN) AND { it["http-status"]?.I()?:0 > 400 } AFTER 20 CHECKS "broken-now-dead"

            }
            "worker" -> {
                val WORKER = it;
                WORKER MACHINE_IS BROKEN IF L(OK, STALE, STARTING) AND { it["http-status"]?.I()?:999 >= 400 && it["http-load"]?.D()?:2.0 < 2.0 } AFTER 30 CHECKS "http-broken"
                WORKER MACHINE_IS BROKEN IF L(OK, STALE, STARTING) AND { it["load"]?.D()?:0.0 > 30 } AFTER 5 CHECKS "mega-overload"
                WORKER MACHINE_IS DEAD IF L(BROKEN) AND { it["http-status"]?.I()?:0 > 400 } AFTER 100 CHECKS "broken-now-dead"

            }
        }
    }
}

public fun snapitoPolicy(infra: Infrastructure, controller: Controller) {
    infra.topology().each {
        defaultTranstitions(it);
        when(it.name()) {
            "lb" -> {
                val balancers = it;
                balancers MACHINE BROKEN RECHECK THEN TELL controller  TO RESTART_MACHINE ;
                balancers MACHINE DEAD RECHECK THEN TELL controller  TO REIMAGE_MACHINE ;
                balancers MACHINE STALE RECHECK THEN  TELL controller TO REIMAGE_MACHINE;
            }
            "gateway" -> {
                val gateways = it;
                gateways MACHINE BROKEN RECHECK THEN TELL controller  TO RESTART_MACHINE;
                gateways MACHINE DEAD RECHECK THEN TELL controller  TO REIMAGE_MACHINE ;
                gateways MACHINE STALE RECHECK THEN TELL controller   TO REIMAGE_MACHINE;
            }
            "worker" -> {
                val workers = it;
                workers MACHINE DEAD RECHECK THEN TELL controller  TO REIMAGE_MACHINE;
                workers BECOME BUSY RECHECK THEN USE controller TO EXPAND;
                workers MACHINE STALE RECHECK THEN TELL controller TO  REIMAGE_MACHINE;
                workers BECOME QUIET RECHECK THEN USE controller  TO CONTRACT;
            }
        }
    }
}

public fun snapitoStrategy(infra: Infrastructure, controller: Controller) {
    infra.topology().each {
        val group = it;
        controller USE { group.failAction(it) { group.reImage(it) } }TO REIMAGE_MACHINE IN_GROUP group;
        controller USE { group.failAction(it) { group.restart(it) } } TO RESTART_MACHINE IN_GROUP group;
        controller WILL { group.expand() } TO EXPAND  UNLESS { group.size() >= group.max }  GROUP group;
        controller WILL { group.contract() } TO CONTRACT UNLESS { group.size() <= group.min } GROUP group;
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

fun buildToplogy(client: DigitalOceanClientFactory): Map<String, DigitalOceanMachineGroup> {
    val gatewaySensorArray = DefaultSensorArray<Any?>(listOf(SSHLoadSensor(), HttpStatusSensor("/gateway?status")));
    val loadBalancerSensorArray = DefaultSensorArray<Any?>(listOf(SSHLoadSensor(), HttpStatusSensor("/")));
    val workerSensorArray = DefaultSensorArray<Any?>(listOf(SSHLoadSensor(), HttpLoadSensor("/api/load"), HttpStatusSensor("/api?url=google.com")));
    val config = DigitalOceanConfig("prod-snapito-", "template-snapito-", 4, 65)

    val loadBalancerGroup = DigitalOceanMachineGroup(client, "lb", loadBalancerSensorArray, config, 2, 2, listOf(), downStreamKonfigurator = HaproxyKonfigurator("/haproxy.cfg.vm"))
    val gatewayGroup = DigitalOceanMachineGroup(client, "gateway", gatewaySensorArray, config, 2, 2, listOf(loadBalancerGroup))
    val workerGroup = DigitalOceanMachineGroup(client, "worker", workerSensorArray, config, 3, 20, listOf(gatewayGroup))
    val map: MutableMap<String, DigitalOceanMachineGroup> = hashMapOf(
            "worker" to workerGroup,
            "gateway" to gatewayGroup,
            "lb" to loadBalancerGroup
    );
    return map;
}


fun main(args: Array<String>): Unit {

    val digitalOceanClient = DigitalOceanClientFactory(System.getProperty("do.cid")?:"", System.getProperty("do.apikey")?:"")
    val groups = buildToplogy(digitalOceanClient);
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


