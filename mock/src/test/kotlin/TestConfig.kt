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

package kontrol.test.mock


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

    group allowMachine (STARTING to OK);
    group allowMachine (STARTING to BROKEN);
    group allowMachine (STARTING to STOPPED);
    group allowMachine (OK to STOPPING);
    group allowMachine (OK to STOPPED);
    group allowMachine (OK to BROKEN);
    group allowMachine (OK to STALE);
    group allowMachine (STOPPING to STOPPED);
    group allowMachine (STOPPING to STARTING);
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


public fun snapitoSensorActions(infra: Infrastructure, controller: Controller) {
    infra.topology().each {
        val group = it;

        group memberIs OK ifStateIn listOf(BROKEN, STARTING) andTest {
            it.data["http-status"]?.I()?:999 < 400 && it.data["load"]?.D()?:0.0 < 30
        } after 5 checks "http-ok"

        group memberIs DEAD ifStateIn listOf(BROKEN) andTest { it.data["http-status"]?.I()?:0 > 400 } after 100 checks "dead"

        when(it.name()) {
            "lb" -> {
                val balancers = it;
                balancers memberIs BROKEN ifStateIn listOf(OK, STALE, STARTING) andTest { it.data["http-status"]?.I()?:222 >= 400 } after 2 checks "http-broken"

                balancers memberIs BROKEN ifStateIn listOf(OK, STALE, STARTING) andTest { it.data["load"]?.D()?:0.0 > 30 } after 2 checks "mega-overload"
                group becomes BUSY ifStateIn listOf(QUIET, NORMAL, null) andTest { it.get("load")?:0.0 > 3.0 }  after 2 checks "overload"
                group becomes QUIET ifStateIn listOf(BUSY, NORMAL, null) andTest { it.get("load")?:1.0 < 1.0 }  after 5 checks "underload"
                group becomes NORMAL ifStateIn listOf(QUIET, BUSY, null) andTest { it.get("load")?:1.0 in 1.0..3.0 }  after 5 checks "group-ok"
            }
            "gateway" -> {

                val gateways = it;
                gateways memberIs BROKEN ifStateIn listOf(OK, STALE, STARTING) andTest {
                    it.data["http-status"]?.I()?:222 >= 400
                } after 3 checks "http-broken"

                gateways memberIs BROKEN ifStateIn listOf(OK, STALE, STARTING) andTest {
                    it.data["load"]?.D()?:0.0 > 30
                } after 3 checks "mega-overload"

                group becomes BUSY ifStateIn listOf(QUIET, NORMAL, null) andTest { it.get("load")?:0.0 > 3.0 }  after 5 checks "overload"

                group becomes QUIET ifStateIn listOf(BUSY, NORMAL, null) andTest { it.get("load")?:1.0 < 1.0 }  after 10 checks "underload"

                group becomes NORMAL ifStateIn listOf(QUIET, BUSY, null) andTest { it.get("load")?:1.0 in 1.0..3.0 }  after 2 checks "group-ok"
            }
            "worker" -> {
                val workers = it;
                workers memberIs BROKEN ifStateIn listOf(OK, STALE, STARTING) andTest { it.data["http-status"]?.I()?:999 >= 400 && it.data["http-load"]?.D()?:2.0 < 2.0 } after 30 checks "http-broken"

                workers memberIs BROKEN ifStateIn listOf(OK, STALE, STARTING) andTest { it.data["load"]?.D()?:0.0 > 30 } after 5 checks "mega-overload"

                group becomes BUSY ifStateIn listOf(BUSY, QUIET, NORMAL, null) andTest { it.get("http-load")?:1.0 > 5.0 || group.activeSize() < group.min }  after 20 checks "overload"

                group becomes QUIET ifStateIn listOf(QUIET, BUSY, NORMAL, null) andTest { it.get("http-load")?:1.0 < 3.0 || group.activeSize() > group.max }  after 60 checks "underload"

                group becomes NORMAL ifStateIn listOf(QUIET, BUSY, null) andTest { it.get("http-load") ?:1.0 in 3.0..5.0 && group.activeSize() in group.min..group.max }  after 5 checks "group-ok"
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
                balancers whenMachine BROKEN recheck THEN tell controller  to RESTART_MACHINE ;
                balancers whenMachine DEAD recheck THEN tell controller  to REIMAGE_MACHINE ;
                balancers whenMachine STALE recheck THEN  tell controller to REIMAGE_MACHINE;
            }
            "gateway" -> {
                val gateways = it;
                gateways whenMachine BROKEN recheck THEN tell controller  to RESTART_MACHINE;
                gateways whenMachine DEAD recheck THEN tell controller  to REIMAGE_MACHINE ;
                gateways whenMachine STALE recheck THEN tell controller   to REIMAGE_MACHINE;
            }
            "worker" -> {
                val workers = it;
                workers whenMachine BROKEN recheck THEN tell controller  to RESTART_MACHINE;
                workers whenMachine DEAD recheck THEN tell controller  to DESTROY_MACHINE;
                workers whenGroup BUSY recheck THEN use controller   to EXPAND;
                workers whenMachine STALE recheck THEN tell controller to  REIMAGE_MACHINE;
                workers whenGroup QUIET recheck THEN use controller  to CONTRACT;
            }
        }
    }
}

public fun snapitoStrategy(infra: Infrastructure, controller: Controller) {
    infra.topology().each {
        when(it.name()) {
            "lb" -> {
                val balancers = it;
                controller will { balancers.failover(it).reImage(it).configure().failback(it) } to REIMAGE_MACHINE inGroup balancers;
                controller will { balancers.failover(it).destroy(it) } to DESTROY_MACHINE inGroup balancers;
                controller will { balancers.failover(it).restart(it).failback(it) } to RESTART_MACHINE inGroup balancers;
            }
            "gateway" -> {
                val gateways = it;
                controller will {
                    gateways.failover(it);
                    infra.topology().get("lb").configure();
                    gateways.reImage(it).failback(it);
                    infra.topology().get("lb").configure()
                } to REIMAGE_MACHINE inGroup gateways;

                controller will {
                    gateways.failover(it).destroy(it);
                    infra.topology().get("lb").configure();
                } to DESTROY_MACHINE inGroup gateways;

                controller will {
                    gateways.failover(it);
                    infra.topology().get("lb").configure();
                    gateways.restart(it).failback(it);
                    infra.topology().get("lb").configure()
                } to RESTART_MACHINE inGroup gateways;
            }
            "worker" -> {
                val workers = it;
                controller will { workers.failover(it).reImage(it) } to REIMAGE_MACHINE inGroup workers;
                controller will { workers.failover(it).destroy(it) } to DESTROY_MACHINE inGroup workers;
                controller will { workers.failover(it).restart(it).failback(it) } to RESTART_MACHINE inGroup workers;
                controller use { workers.expand() } to EXPAND  unless { workers.activeSize() > workers.max }  group workers;
                controller use { workers.contract() } to CONTRACT unless { workers.activeSize() < workers.min } group workers;
            }
        }
    };
}



