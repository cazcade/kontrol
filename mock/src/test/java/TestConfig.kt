package kontrol.test


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

        group MACHINE_IS OK IF listOf(BROKEN, STARTING) AND {
            it.data["http-status"]?.I()?:999 < 400 && it.data["load"]?.D()?:0.0 < 30
        } AFTER 5 CHECKS "http-ok"

        group MACHINE_IS DEAD IF listOf(BROKEN) AND { it.data["http-status"]?.I()?:0 > 400 } AFTER 100 CHECKS "dead"

        when(it.name()) {
            "lb" -> {
                val balancers = it;
                balancers MACHINE_IS BROKEN IF listOf(OK, STALE, STARTING) AND { it.data["http-status"]?.I()?:222 >= 400 } AFTER 2 CHECKS "http-broken"

                balancers MACHINE_IS BROKEN IF listOf(OK, STALE, STARTING) AND { it.data["load"]?.D()?:0.0 > 30 } AFTER 2 CHECKS "mega-overload"
                group IS BUSY IF listOf(QUIET, NORMAL, null) AND { it.get("load")?:0.0 > 3.0 }  AFTER 2 CHECKS "overload"
                group IS QUIET IF listOf(BUSY, NORMAL, null) AND { it.get("load")?:1.0 < 1.0 }  AFTER 5 CHECKS "underload"
                group IS NORMAL IF listOf(QUIET, BUSY, null) AND { it.get("load")?:1.0 in 1.0..3.0 }  AFTER 5 CHECKS "group-ok"
            }
            "gateway" -> {

                val gateways = it;
                gateways MACHINE_IS BROKEN IF listOf(OK, STALE, STARTING) AND {
                    it.data["http-status"]?.I()?:222 >= 400
                } AFTER 3 CHECKS "http-broken"

                gateways MACHINE_IS BROKEN IF listOf(OK, STALE, STARTING) AND {
                    it.data["load"]?.D()?:0.0 > 30
                } AFTER 3 CHECKS "mega-overload"

                group IS BUSY IF listOf(QUIET, NORMAL, null) AND { it.get("load")?:0.0 > 3.0 }  AFTER 5 CHECKS "overload"

                group IS QUIET IF listOf(BUSY, NORMAL, null) AND { it.get("load")?:1.0 < 1.0 }  AFTER 10 CHECKS "underload"

                group IS NORMAL IF listOf(QUIET, BUSY, null) AND { it.get("load")?:1.0 in 1.0..3.0 }  AFTER 2 CHECKS "group-ok"
            }
            "worker" -> {
                val workers = it;
                workers MACHINE_IS BROKEN IF listOf(OK, STALE, STARTING) AND { it.data["http-status"]?.I()?:999 >= 400 && it.data["http-load"]?.D()?:2.0 < 2.0 } AFTER 30 CHECKS "http-broken"

                workers MACHINE_IS BROKEN IF listOf(OK, STALE, STARTING) AND { it.data["load"]?.D()?:0.0 > 30 } AFTER 5 CHECKS "mega-overload"

                group IS BUSY IF listOf(BUSY, QUIET, NORMAL, null) AND { it.get("http-load")?:1.0 > 5.0 || group.activeSize() < group.min }  AFTER 20 CHECKS "overload"

                group IS QUIET IF listOf(QUIET, BUSY, NORMAL, null) AND { it.get("http-load")?:1.0 < 3.0 || group.activeSize() > group.max }  AFTER 60 CHECKS "underload"

                group IS NORMAL IF listOf(QUIET, BUSY, null) AND { it.get("http-load") ?:1.0 in 3.0..5.0 && group.activeSize() in group.min..group.max }  AFTER 5 CHECKS "group-ok"
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
                workers MACHINE BROKEN RECHECK THEN TELL controller  TO RESTART_MACHINE;
                workers MACHINE DEAD RECHECK THEN TELL controller  TO DESTROY_MACHINE;
                workers BECOME BUSY RECHECK THEN USE controller   TO EXPAND;
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
                controller WILL { workers.expand() } TO EXPAND  UNLESS { workers.activeSize() > workers.max }  GROUP workers;
                controller WILL { workers.contract() } TO CONTRACT UNLESS { workers.activeSize() < workers.min } GROUP workers;
            }
        }
    };
}



