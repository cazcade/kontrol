package kontrol.examples.docean

import kontrol.impl.ocean.DigitalOceanMachineGroup
import kontrol.impl.DigitalOceanCloud
import kontrol.impl.ocean.DigitalOceanClientFactory
import kontrol.impl.DefaultController
import kontrol.impl.sensor.SSHLoadSensor
import kontrol.impl.sensor.HttpStatusSensor
import kontrol.impl.ocean.DigitalOceanConfig
import kontrol.impl.sensor.HttpLoadSensor
import kontrol.impl.DefaultSensorArray
import kontrol.api.Infrastructure
import kontrol.api.MachineState.*
import kontrol.api.MachineGroupState.*
import kontrol.api.Action.*
import kontrol.api.GroupAction.*
import kontrol.api.Controller
import kontrol.api.MachineGroup
import kontrol.api.MachineGroup.Recheck.*;
import kontrol.impl.L

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
        GROUP MACHINE_IS DEAD IF L(BROKEN) AND { it["http-status"]?.I()?:0 > 400 } AFTER 100 CHECKS "broken-now-dead"
        GROUP MACHINE_IS DEAD IF L(STOPPED) AFTER 50 CHECKS "stopped-now-dead"

        GROUP IS BUSY IF L(QUIET, BUSY, NORMAL, null) AND { it["http-load"]?:0.0 > 6.0 || GROUP.size() < GROUP.min }  AFTER 20 CHECKS "overload"
        GROUP IS QUIET IF L(QUIET, BUSY, NORMAL, null) AND { it["http-load"]?:6.0 < 3.0 || GROUP.size() > GROUP.max }  AFTER 100 CHECKS "underload"
        GROUP IS NORMAL IF L(QUIET, BUSY, null) AND { it["http-load"]?:1.0 in 3.0..6.0 && GROUP.size() in GROUP.min..GROUP.max }  AFTER 5 CHECKS "group-ok"

        when(it.name()) {
            "lb" -> {
                val BALANCER = it;
                BALANCER MACHINE_IS BROKEN IF L(OK, STALE, STARTING) AND { it["http-status"]?.I()?:999 >= 400 } AFTER 5 CHECKS "http-broken"
                BALANCER MACHINE_IS BROKEN IF L(OK, STALE, STARTING) AND { it["load"]?.D()?:0.0 > 30 } AFTER 2 CHECKS "mega-overload"

            }
            "gateway" -> {

                val GATEWAY = it;
                GATEWAY MACHINE_IS BROKEN IF L(OK, STALE, STARTING) AND { it["http-status"]?.I()?:999 >= 400 } AFTER 3 CHECKS "http-broken"
                GATEWAY MACHINE_IS BROKEN IF L(OK, STALE, STARTING) AND { it["load"]?.D()?:0.0 > 30 } AFTER 3 CHECKS "mega-overload"

            }
            "worker" -> {
                val WORKER = it;
                WORKER MACHINE_IS BROKEN IF L(OK, STALE, STARTING) AND { it["http-status"]?.I()?:999 >= 400 && it["http-load"]?.D()?:2.0 < 2.0 } AFTER 30 CHECKS "http-broken"
                WORKER MACHINE_IS BROKEN IF L(OK, STALE, STARTING) AND { it["load"]?.D()?:0.0 > 30 } AFTER 5 CHECKS "mega-overload"

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
        when(it.name()) {
            "lb" -> {
                val balancers = it;
                controller USE { balancers.failover(it).reImage(it).configure().failback(it) } TO REIMAGE_MACHINE IN_GROUP balancers;
                controller USE { balancers.failover(it).destroy(it) } TO DESTROY_MACHINE IN_GROUP balancers;
                controller USE { balancers.failover(it).restart(it).failback(it) } TO RESTART_MACHINE IN_GROUP balancers;
            }
            "gateway" -> {
                val gateways = it;
                controller USE {
                    gateways.failover(it);
                    infra.topology().get("lb").configure();
                    gateways.reImage(it).failback(it);
                    infra.topology().get("lb").configure()
                } TO REIMAGE_MACHINE IN_GROUP gateways;

                controller USE {
                    gateways.failover(it).destroy(it);
                    infra.topology().get("lb").configure();
                } TO DESTROY_MACHINE IN_GROUP gateways;

                controller USE {
                    gateways.failover(it);
                    infra.topology().get("lb").configure();
                    gateways.restart(it).failback(it);
                    infra.topology().get("lb").configure()
                } TO RESTART_MACHINE IN_GROUP gateways;
            }
            "worker" -> {
                val workers = it;
                controller USE { workers.failover(it).reImage(it) } TO REIMAGE_MACHINE IN_GROUP workers;
                controller USE { workers.failover(it).destroy(it) } TO DESTROY_MACHINE IN_GROUP workers;
                controller USE { workers.failover(it).restart(it).failback(it) } TO RESTART_MACHINE IN_GROUP workers;
                controller WILL { workers.expand() } TO EXPAND  UNLESS { workers.size() > workers.max }  GROUP workers;
                controller WILL { workers.contract() } TO CONTRACT UNLESS { workers.size() < workers.min } GROUP workers;
            }
        }
    };
}

fun buildToplogy(client: DigitalOceanClientFactory): Map<String, DigitalOceanMachineGroup> {
    val gatewaySensorArray = DefaultSensorArray<Any?>(listOf(SSHLoadSensor(), HttpStatusSensor("/gateway?status")));
    val loadBalancerSensorArray = DefaultSensorArray<Any?>(listOf(SSHLoadSensor(), HttpStatusSensor("/")));
    val workerSensorArray = DefaultSensorArray<Any?>(listOf(SSHLoadSensor(), HttpLoadSensor("/api/load"), HttpStatusSensor("/api?url=google.com")));
    val config = DigitalOceanConfig("prod-snapito-", "template-snapito-", 4, 65)
    val map: MutableMap<String, DigitalOceanMachineGroup> = hashMapOf(
            "worker" to DigitalOceanMachineGroup(client, "worker", workerSensorArray, config, 3, 20),
            "gateway" to DigitalOceanMachineGroup(client, "gateway", gatewaySensorArray, config, 2, 2),
            "lb" to DigitalOceanMachineGroup(client, "lb", loadBalancerSensorArray, config, 2, 2)
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


