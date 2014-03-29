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

package com.cazcade.snapito.kontrol.worker

import kontrol.digitalocean.DigitalOceanMachineGroup
import kontrol.digitalocean.DigitalOceanClientFactory
import kontrol.sensor.SSHLoadSensor
import kontrol.digitalocean.DigitalOceanConfig
import kontrol.common.DefaultSensorArray
import kontrol.api.Infrastructure
import kontrol.sensor.HttpResponseTimeSensor
import kontrol.postmortem.CentosPostmortem
import kontrol.server.Server
import kontrol.api.Controller
import kontrol.digitalocean.StaticMachine
import java.util.ArrayList
import kontrol.api.sensors.SensorArray
import kontrol.api.MachineGroup
import kontrol.staticmc.MixedCloud
import kontrol.postmortem.JettyPostmortem
import kontrol.postmortem.HeapDumpsPostmortem
import kontrol.impl.sensor.DiskUsageSensor
import kontrol.api.PostmortemStore
import kontrol.api.Machine
import kontrol.api.MachineGroup.Recheck.*
import kontrol.api.MachineGroupState.*
import kontrol.api.GroupAction.*
import kontrol.api.Action.*
import kontrol.api.MachineState.*
import kontrol.common.group.ext.allowDefaultTransitions
import kontrol.sensor.HttpStatusSensor
import kontrol.common.DefaultGroupSensorArray
import kontrol.common.group.ext.addGroupSensorRules


public fun MachineGroup.configureDefaultActions(controller: Controller, postmortemStore: PostmortemStore, upgradeAction: (Machine, MachineGroup) -> Unit = { m, g -> ;g.rebuild(m) }, downgradeAction: (Machine, MachineGroup) -> Unit = { m, g -> }) {

    //nb: All actions listed under takeActions must be atomic, don't assume you can add together multiple actions to form an atomic action.
    //nb: This is because the multiple actions are not guaranteed to both be executed or even their order!

    this whenMachine BROKEN recheck THEN tell controller takeActions listOf(FIX);
    this whenMachine OK recheck THEN tell controller takeAction FAILBACK;
    this whenMachine DEAD recheck THEN tell controller takeActions listOf(REBUILD) ;
    this whenMachine STALE recheck THEN tell controller takeActions listOf(UPGRADE);
    this whenMachine UPGRADE_FAILED recheck THEN tell controller takeActions listOf(DOWNGRADE);
    this whenMachine FAILED recheck THEN tell controller takeActions listOf(DESTROY_MACHINE);
    this whenGroup BUSY recheck THEN use controller takeAction EXPAND;
    this whenGroup QUIET recheck THEN use controller takeAction CONTRACT;
    this whenGroup GROUP_BROKEN recheck THEN use controller  takeActions listOf(EXPAND);

    //make sure the action performed is atomic as multiple actions can be split

    controller will {
        this.failover(it);
        if (this.workingSize() > 1 ) {
            upgradeAction(it, this);
        } else {
            println("Upgrade skipped, not enough working machines.")
        }
        java.lang.String()
    } takeAction UPGRADE IF { this.workingSize() > 1 && it.state() in listOf(STALE) }  inGroup this;

    controller will {
        downgradeAction(it, this);
        java.lang.String()
    } takeAction DOWNGRADE inGroup this;

    controller will {
        this.failback(it);
        java.lang.String()
    } takeAction FAILBACK IF { it.state() in listOf(OK, STALE, OVERLOADED, null) } inGroup this;

    controller will {
        this.failover(it);
        this.rebuild(it);
        try {
            downgradeAction(it, this);
        } catch(e: Exception) {
            e.printStackTrace()
        }
        this.clearState(it);
        this.configure(it)
        java.lang.String()
    } takeAction REBUILD IF { it.state() !in listOf(OK, STALE, OVERLOADED, null) } inGroup this;

    controller will {
        this.failover(it);
        postmortemStore.addAll(this.postmortem(it));
        try {
            downgradeAction(it, this);
        } catch(e: Exception) {
            e.printStackTrace()
        }
        this.fix(it);
        this.configure(it);
        java.lang.String()
    } takeAction FIX IF { this.other(it) != null && it.state() !in listOf(null, OK) } inGroup this;

    controller will {
        this.failover(it);
        this.destroy(it);
        java.lang.String()
    } takeAction DESTROY_MACHINE  IF { this.other(it) != null && this.workingSize() > this.min }  inGroup this;

    controller use {
        downgradeAction(this.configure(this.expand()), this);
        java.lang.String()
    } to EXPAND  IF { this.machines().size < this.hardMax && this.workingSize() < this.max }  group this;

    controller use {
        this.contract();
        java.lang.String()
    } to CONTRACT IF { this.workingSize() > this.min } group this;

    controller use {
        this.enabled = false;
        try {
            try {
                it.brokenMachines().sortBy { it.id() } forEach {
                    this.rebuild(it);
                    try {
                        downgradeAction(it, this);
                    } catch(e: Exception) {
                        e.printStackTrace()
                    }
                    this.clearState(it);
                    this.configure(it)
                }
                this.configure();
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (this.workingSize() == 0) {
                    expand();
                }
            }
            java.lang.String()
        } finally {
            this.enabled = true
        }
    } to EMERGENCY_FIX  IF { this.workingSize() == 0 } group this;
}


public fun MachineGroup.applyDefaultRules(timeFactor: Int = 60) {
    this memberIs STALE ifStateIn listOf(OK, OVERLOADED, null) andTest { this.other(it) != null && this.machines().filter { it.state() == STALE }.size() == 0 } after  timeFactor * 60000  seconds "upgrade"
    this memberIs BROKEN ifStateIn listOf(BROKEN, UPGRADE_FAILED) after timeFactor * 10 seconds "bad-now-broken"
    this memberIs BROKEN ifStateIn listOf(OVERLOADED) after timeFactor * 10 seconds "overloaded-now-broken"
    this memberIs DEAD ifStateIn listOf(DEAD, BROKEN, STOPPED, null, UPGRADE_FAILED) after timeFactor * 15 seconds "escalate-broken-to-dead"
    val escalateDuration: Long = 60L * timeFactor * 1000L
    this memberIs DEAD ifStateIn listOf(DEAD, BROKEN, STOPPED, UPGRADE_FAILED) andTest { it.fsm.history.percentageInWindow(listOf(UPGRADE_FAILED, BROKEN, STOPPED, DEAD), (escalateDuration / 2)..escalateDuration) > 30.0 } after timeFactor seconds "flap-now-escalate-to-dead"
    this memberIs FAILED ifStateIn listOf(DEAD, FAILED) after timeFactor * 10 seconds "dead-now-failed"
    this memberIs FAILED ifStateIn listOf(FAILED, DEAD) andTest { it.fsm.history.percentageInWindow(listOf(DEAD, FAILED), (escalateDuration / 2)..escalateDuration) > 40.0 } after timeFactor seconds "flap-now-escalate-to-failed"
}

public fun MachineGroup.addMachineOverloadRules(vararg rules: Pair<String, Double>, timeFactor: Int = 60) {

    this memberIs OK ifStateIn listOf(OVERLOADED) andTest { machine -> rules.all { machine[it.first]?.D()?:(it.second + 1) < it.second } } after timeFactor / 2 seconds "machine-ok-load"

    this memberIs OVERLOADED ifStateIn listOf(OK, null) andTest { machine -> rules.any { machine[it.first]?.D()?:(it.second - 1) > it.second } } after timeFactor * 4 seconds "machine-overloaded"
}

public fun MachineGroup.addMachineBrokenRules(vararg rules: Pair<String, Double>, timeFactor: Int = 60) {


    this memberIs OK ifStateIn listOf(UPGRADE_FAILED, BROKEN, null, DEAD, STOPPED, FAILED, BROKEN) andTest { machine -> rules.all { machine[it.first]?.D()?:(it.second + 1) < it.second } } after timeFactor / 2 seconds "machine-ok"

    this memberIs OK ifStateIn listOf(UPGRADE_FAILED, STALE) andTest { machine -> rules.all { machine[it.first]?.D()?:(it.second + 1) < it.second } } after timeFactor * 10 seconds "machine-ok"


    this memberIs UPGRADE_FAILED ifStateIn listOf(STALE) andTest { machine -> rules.any { machine[it.first]?.D()?:(it.second - 1) > it.second } } after  timeFactor * 10 seconds "downgrade"

    this memberIs BROKEN ifStateIn listOf(OK, OVERLOADED, BROKEN, STOPPED, null) andTest { machine -> rules.any { machine[it.first]?.D()?:(it.second - 1) > it.second } && this.downStreamGroups.all { it.state() != GROUP_BROKEN } } after timeFactor * 3 seconds "machine-broken"
}


public fun snapitoSensorActions(infra: Infrastructure): Infrastructure {
    infra.topology().each { group ->
        group.addMachineBrokenRules("http-status" to 399.0)
        when(group.name()) {
            "worker" -> {
                //change this when deployed
                group.addMachineOverloadRules("load" to 3.0, "http-response-time" to 500.0)
                group.addGroupSensorRules("http-response-time" to -1.0..1500.0, "load" to 1.0..3.0, "redis-snapito_snap_queue:linux" to  50.0..500.0)
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
    val workerSensorArray = DefaultSensorArray(listOf(SSHLoadSensor(), HttpStatusSensor("/"), HttpResponseTimeSensor("/"), DiskUsageSensor()));
    val workerConfig = DigitalOceanConfig(if (test) "test-snapito-" else "prod-snapito-", "template-snapito-", 4, 62)
    val keys = "93676"
    val workerGroup = DigitalOceanMachineGroup(client, controller, "worker", workerSensorArray, workerConfig, keys, 0, 24, 32, arrayListOf<MachineGroup>(), listOf(CentosPostmortem(), JettyPostmortem("/home/cazcade/jetty"), HeapDumpsPostmortem()), groupSensors = DefaultGroupSensorArray(listOf(RedisListSensor("snapito_snap_queue:linux", "107.170.24.38", 6379, autoTrimAt = 50000))))
    return hashMapOf("worker" to workerGroup);
}


fun main(args: Array<String>): Unit {

    val server = Server { controller, bus, postmortems ->
        val client = DigitalOceanClientFactory(System.getProperty("do.cid")?:"", System.getProperty("do.apikey")?:"")
        val cloud = MixedCloud(buildGroups(client, controller, false))
        cloud.topology().each { group ->
            group.allowDefaultTransitions();
            group.applyDefaultRules(60);
        }
        cloud["worker"].configureDefaultActions(controller, postmortems)
        snapitoSensorActions(cloud);
    }

    server.start(30)
    try {

        while (true) {
            Thread.sleep(10000)
        }
    } finally {
        server.stop();
    }

}


