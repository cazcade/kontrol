package kontrol_examples

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

/**
 * @author <a href="http://uk.linkedin.com/in/neilellis">Neil Ellis</a>
 */


public fun defaultTranstitions(group: MachineGroup) {

    group allowMachine (MACHINE_STARTING to MACHINE_OK);
    group allowMachine (MACHINE_STARTING to BROKEN_MACHINE);
    group allowMachine (MACHINE_STARTING to MACHINE_STOPPED);
    group allowMachine (MACHINE_OK to MACHINE_STOPPING);
    group allowMachine (MACHINE_OK to MACHINE_STOPPED);
    group allowMachine (MACHINE_OK to BROKEN_MACHINE);
    group allowMachine (MACHINE_OK to STALE_MACHINE);
    group allowMachine (MACHINE_STOPPING to MACHINE_STOPPED);
    group allowMachine (MACHINE_STOPPING to MACHINE_STARTING);
    group allowMachine (MACHINE_STOPPED to DEAD_MACHINE);
    group allowMachine (BROKEN_MACHINE to MACHINE_STOPPING);
    group allowMachine (BROKEN_MACHINE to MACHINE_STOPPED);
    group allowMachine (BROKEN_MACHINE to MACHINE_OK);
    group allowMachine (BROKEN_MACHINE to DEAD_MACHINE);
    group allowMachine (STALE_MACHINE to  MACHINE_STOPPING);

    group allow (UNDERLOADED to OVERLOADED);
    group allow (UNDERLOADED to GROUP_OK);
    group allow (UNDERLOADED to GROUP_BROKEN);
    group allow (OVERLOADED to GROUP_OK);
    group allow (OVERLOADED to UNDERLOADED);
    group allow (OVERLOADED to  GROUP_BROKEN);
    group allow (GROUP_OK to GROUP_BROKEN);
    group allow (GROUP_OK to UNDERLOADED);
    group allow (GROUP_OK to OVERLOADED);
    group allow (GROUP_BROKEN to UNDERLOADED);
    group allow (GROUP_BROKEN to OVERLOADED);
    group allow (GROUP_BROKEN to GROUP_OK);
}


public fun snapitoSensorActions(infra: Infrastructure) {
    infra.topology().each {
        val group = it;

        group MACHINE_STATE MACHINE_OK FROM listOf(BROKEN_MACHINE, MACHINE_STARTING) WHEN {
            it.sensorData["http-status"]?.toInt()?:999 < 400 && it.sensorData["load"]?.toDouble()?:0.0 < 30
        } EVERY 5 CALL_THIS "http-ok"

        group MACHINE_STATE DEAD_MACHINE FROM listOf(BROKEN_MACHINE) WHEN { it.sensorData["http-status"]?.toInt()?:0 > 400 } EVERY 100 CALL_THIS "dead"

        group MACHINE_STATE DEAD_MACHINE FROM listOf(MACHINE_STOPPED) WHEN { true } EVERY 10 CALL_THIS "stopped-now-dead"

        when(it.name()) {
            "lb" -> {
                val balancers = it;
                balancers MACHINE_STATE BROKEN_MACHINE FROM listOf(MACHINE_OK, STALE_MACHINE, MACHINE_STARTING) WHEN { it.sensorData["http-status"]?.toInt()?:222 >= 400 } EVERY 2 CALL_THIS "http-broken"

                balancers MACHINE_STATE BROKEN_MACHINE FROM listOf(MACHINE_OK, STALE_MACHINE, MACHINE_STARTING) WHEN { it.sensorData["load"]?.toDouble()?:0.0 > 30 } EVERY 2 CALL_THIS "mega-overload"
                group STATE OVERLOADED FROM listOf(UNDERLOADED, GROUP_OK, null) WHEN { it.sensorArray.avg(it.machines().map { it.sensorData["load"] })?:0.0 > 3.0 }  EVERY 2 CALL_THIS "overload"
                group STATE UNDERLOADED FROM listOf(OVERLOADED, GROUP_OK, null) WHEN { it.sensorArray.avg(it.machines().map { it.sensorData["load"] })?:1.0 < 1.0 }  EVERY 5 CALL_THIS "underload"
                group STATE GROUP_OK FROM listOf(UNDERLOADED, OVERLOADED, null) WHEN { it.sensorArray.avg(it.machines().map { it.sensorData["load"] })?:1.0 in 1.0..3.0 }  EVERY 5 CALL_THIS "group-ok"
            }
            "gateway" -> {

                val gateways = it;
                gateways MACHINE_STATE BROKEN_MACHINE FROM listOf(MACHINE_OK, STALE_MACHINE, MACHINE_STARTING) WHEN {
                    it.sensorData["http-status"]?.toInt()?:222 >= 400
                } EVERY 3 CALL_THIS "http-broken"

                gateways MACHINE_STATE BROKEN_MACHINE FROM listOf(MACHINE_OK, STALE_MACHINE, MACHINE_STARTING) WHEN {
                    it.sensorData["load"]?.toDouble()?:0.0 > 30
                } EVERY 3 CALL_THIS "mega-overload"

                group STATE OVERLOADED FROM listOf(UNDERLOADED, GROUP_OK, null) WHEN {
                    it.sensorArray.avg(it.machines().map { it.sensorData["load"] })?:0.0 > 3.0
                }  EVERY 5 CALL_THIS "overload"

                group STATE UNDERLOADED FROM listOf(OVERLOADED, GROUP_OK, null) WHEN {
                    it.sensorArray.avg(it.machines().map { it.sensorData["load"] })?:1.0 < 1.0
                }  EVERY 10 CALL_THIS "underload"

                group STATE GROUP_OK FROM listOf(UNDERLOADED, OVERLOADED, null) WHEN {
                    it.sensorArray.avg(it.machines().map { it.sensorData["load"] })?:1.0 in 1.0..3.0
                }  EVERY 2 CALL_THIS "group-ok"
            }
            "worker" -> {
                val workers = it;
                workers MACHINE_STATE BROKEN_MACHINE FROM listOf(MACHINE_OK, STALE_MACHINE, MACHINE_STARTING) WHEN { it.sensorData["http-status"]?.toInt()?:999 >= 400 && it.sensorData["http-load"]?.toDouble()?:2.0 < 2.0 } EVERY 30 CALL_THIS "http-broken"

                workers MACHINE_STATE BROKEN_MACHINE FROM listOf(MACHINE_OK, STALE_MACHINE, MACHINE_STARTING) WHEN { it.sensorData["load"]?.toDouble()?:0.0 > 30 } EVERY 5 CALL_THIS "mega-overload"

                group STATE OVERLOADED FROM listOf(OVERLOADED, UNDERLOADED, GROUP_OK, null) WHEN {
                    it.sensorArray. avg(
                            it.machines() filter { it.state() == MACHINE_OK } map { it.sensorData["http-load"] }
                    )?:0.0 > 3.0
                    || group.size() < group.minSize
                }  EVERY 20 CALL_THIS "overload"

                group STATE UNDERLOADED FROM listOf(UNDERLOADED, OVERLOADED, GROUP_OK, null) WHEN {
                    it.sensorArray.avg(
                            it.machines() filter { it.state() == MACHINE_OK } map { it.sensorData["http-load"] }
                    )?:3.0 < 1.0 || group.size() > group.maxSize
                }  EVERY 100 CALL_THIS "underload"

                group STATE GROUP_OK FROM listOf(UNDERLOADED, OVERLOADED, null) WHEN {
                    it.sensorArray.avg(
                            it.machines() filter { it.state() == MACHINE_OK } map { it.sensorData["http-load"] }
                    )?:1.0 in 1.0..3.0 && group.size() in group.minSize..group.maxSize
                }  EVERY 5 CALL_THIS "group-ok"
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
                balancers HAVE BROKEN_MACHINE RECHECK THEN TELL controller  TO RESTART_MACHINE ;
                balancers HAVE DEAD_MACHINE RECHECK THEN TELL controller  TO REIMAGE_MACHINE ;
                balancers HAVE STALE_MACHINE RECHECK THEN  TELL controller TO REIMAGE_MACHINE;
            }
            "gateway" -> {
                val gateways = it;
                gateways HAVE BROKEN_MACHINE RECHECK THEN TELL controller  TO RESTART_MACHINE;
                gateways HAVE DEAD_MACHINE RECHECK THEN TELL controller  TO REIMAGE_MACHINE ;
                gateways HAVE STALE_MACHINE RECHECK THEN TELL controller   TO REIMAGE_MACHINE;
            }
            "worker" -> {
                val workers = it;
                workers HAVE BROKEN_MACHINE RECHECK THEN TELL controller  TO RESTART_MACHINE;
                workers HAVE DEAD_MACHINE RECHECK THEN TELL controller  TO REIMAGE_MACHINE;
                workers ARE OVERLOADED RECHECK THEN USE controller   TO EXPAND;
                workers HAVE STALE_MACHINE RECHECK THEN TELL controller TO  REIMAGE_MACHINE;
                workers ARE UNDERLOADED RECHECK THEN USE controller  TO CONTRACT;
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
                    infra.topology().group("lb").configure();
                    gateways.reImage(it).failback(it);
                    infra.topology().group("lb").configure()
                } TO REIMAGE_MACHINE IN_GROUP gateways;

                controller USE {
                    gateways.failover(it).destroy(it);
                    infra.topology().group("lb").configure();
                } TO DESTROY_MACHINE IN_GROUP gateways;

                controller USE {
                    gateways.failover(it);
                    infra.topology().group("lb").configure();
                    gateways.restart(it).failback(it);
                    infra.topology().group("lb").configure()
                } TO RESTART_MACHINE IN_GROUP gateways;
            }
            "worker" -> {
                val workers = it;
                controller USE { workers.failover(it).reImage(it) } TO REIMAGE_MACHINE IN_GROUP workers;
                controller USE { workers.failover(it).destroy(it) } TO DESTROY_MACHINE IN_GROUP workers;
                controller USE { workers.failover(it).restart(it).failback(it) } TO RESTART_MACHINE IN_GROUP workers;
                controller WILL { workers.expand() } TO EXPAND  UNLESS { workers.size() > workers.maxSize }  GROUP workers;
                controller WILL { workers.contract() } TO CONTRACT UNLESS { workers.size() < workers.minSize } GROUP workers;
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


