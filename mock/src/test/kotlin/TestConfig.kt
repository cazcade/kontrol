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

    group allowMachine (OK to STOPPED);
    group allowMachine (OK to BROKEN);
    group allowMachine (OK to STALE);
    group allowMachine (BROKEN to STOPPED);
    group allowMachine (BROKEN to OK);
    group allowMachine (BROKEN to DEAD);

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

        group memberIs OK ifStateIn listOf(BROKEN) andTest {
            it.data["http-status"]?.lastEntry()?.I()?:999 < 400 && it["load"]?.D()?:0.0 < 30
        } after 5 seconds "http-ok"

        group memberIs DEAD ifStateIn listOf(BROKEN) andTest { it["http-status"]?.I()?:0 > 400 } after 100 seconds "dead"

        when(it.name()) {
            "lb" -> {
                val balancers = it;
                balancers memberIs BROKEN ifStateIn listOf(OK, STALE) andTest { it["http-status"]?.I()?:222 >= 400 } after 2 seconds "http-broken"

                balancers memberIs BROKEN ifStateIn listOf(OK, STALE) andTest { it["load"]?.D()?:0.0 > 30 } after 2 seconds "mega-overload"
                group becomes BUSY ifStateIn listOf(QUIET, NORMAL, null) andTest { it.get("load")?:0.0 > 3.0 }  after 2 seconds "overload"
                group becomes QUIET ifStateIn listOf(BUSY, NORMAL, null) andTest { it.get("load")?:1.0 < 1.0 }  after 5 seconds "underload"
                group becomes NORMAL ifStateIn listOf(QUIET, BUSY, null) andTest { it.get("load")?:1.0 in 1.0..3.0 }  after 5 seconds "group-ok"
            }
            "gateway" -> {

                val gateways = it;
                gateways memberIs BROKEN ifStateIn listOf(OK, STALE) andTest {
                    it["http-status"]?.I()?:222 >= 400
                } after 3 seconds "http-broken"

                gateways memberIs BROKEN ifStateIn listOf(OK, STALE) andTest {
                    it["load"]?.D()?:0.0 > 30
                } after 3 seconds "mega-overload"

                group becomes BUSY ifStateIn listOf(QUIET, NORMAL, null) andTest { it.get("load")?:0.0 > 3.0 }  after 5 seconds "overload"

                group becomes QUIET ifStateIn listOf(BUSY, NORMAL, null) andTest { it.get("load")?:1.0 < 1.0 }  after 10 seconds "underload"

                group becomes NORMAL ifStateIn listOf(QUIET, BUSY, null) andTest { it.get("load")?:1.0 in 1.0..3.0 }  after 2 seconds "group-ok"
            }
            "worker" -> {
                val workers = it;
                workers memberIs BROKEN ifStateIn listOf(OK, STALE) andTest { it["http-status"]?.I()?:999 >= 400 && it["http-load"]?.D()?:2.0 < 2.0 } after 30 seconds "http-broken"

                workers memberIs BROKEN ifStateIn listOf(OK, STALE) andTest { it["load"]?.D()?:0.0 > 30 } after 5 seconds "mega-overload"

                group becomes BUSY ifStateIn listOf(BUSY, QUIET, NORMAL, null) andTest { it.get("http-load")?:1.0 > 5.0 || group.activeSize() < group.min }  after 20 seconds "overload"

                group becomes QUIET ifStateIn listOf(QUIET, BUSY, NORMAL, null) andTest { it.get("http-load")?:1.0 < 3.0 || group.activeSize() > group.max }  after 60 seconds "underload"

                group becomes NORMAL ifStateIn listOf(QUIET, BUSY, null) andTest { it.get("http-load") ?:1.0 in 3.0..5.0 && group.activeSize() in group.min..group.max }  after 5 seconds "group-ok"
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
                balancers whenMachine BROKEN recheck THEN tell controller  takeAction FIX ;
                balancers whenMachine DEAD recheck THEN tell controller  takeAction REBUILD ;
                balancers whenMachine STALE recheck THEN  tell controller takeAction REBUILD;
            }
            "gateway" -> {
                val gateways = it;
                gateways whenMachine BROKEN recheck THEN tell controller  takeAction FIX;
                gateways whenMachine DEAD recheck THEN tell controller  takeAction REBUILD ;
                gateways whenMachine STALE recheck THEN tell controller   takeAction REBUILD;
            }
            "worker" -> {
                val workers = it;
                workers whenMachine BROKEN recheck THEN tell controller  takeAction FIX;
                workers whenMachine DEAD recheck THEN tell controller  takeAction DESTROY_MACHINE;
                workers whenGroup BUSY recheck THEN use controller   takeAction EXPAND;
                workers whenMachine STALE recheck THEN tell controller takeAction  REBUILD;
                workers whenGroup QUIET recheck THEN use controller  takeAction CONTRACT;
            }
        }
    }
}

public fun snapitoStrategy(infra: Infrastructure, controller: Controller) {
    infra.topology().each {
        when(it.name()) {
            "lb" -> {
                val balancers = it;
                controller will { balancers.failover(it).rebuild(it).configure().failback(it);java.lang.String() } takeAction REBUILD inGroup balancers;
                controller will { balancers.failover(it).destroy(it);java.lang.String() } takeAction DESTROY_MACHINE inGroup balancers;
                controller will { balancers.failover(it).restart(it).failback(it);java.lang.String() } takeAction FIX inGroup balancers;
            }
            "gateway" -> {
                val gateways = it;
                controller will {
                    gateways.failover(it);
                    infra.topology().get("lb").configure();
                    gateways.rebuild(it).failback(it);
                    infra.topology().get("lb").configure()
                    ;java.lang.String()
                } takeAction REBUILD inGroup gateways;

                controller will {
                    gateways.failover(it).destroy(it);
                    infra.topology().get("lb").configure();
                    ;java.lang.String()
                } takeAction DESTROY_MACHINE inGroup gateways;

                controller will {
                    gateways.failover(it);
                    infra.topology().get("lb").configure();
                    gateways.restart(it).failback(it);
                    infra.topology().get("lb").configure()
                    ;java.lang.String()
                } takeAction FIX inGroup gateways;
            }
            "worker" -> {
                val workers = it;
                controller will { workers.failover(it).rebuild(it);java.lang.String() } takeAction REBUILD inGroup workers;
                controller will { workers.failover(it).destroy(it);java.lang.String() } takeAction DESTROY_MACHINE inGroup workers;
                controller will { workers.failover(it).restart(it).failback(it) ;java.lang.String() } takeAction FIX inGroup workers;
                controller use { workers.expand() ;java.lang.String() } to EXPAND  IF { workers.activeSize() < workers.max }  group workers;
                controller use { workers.contract();java.lang.String() } to CONTRACT IF { workers.activeSize() > workers.min } group workers;
            }
        }
    };
}



