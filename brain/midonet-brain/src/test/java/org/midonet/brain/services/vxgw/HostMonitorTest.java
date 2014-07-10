/*
 * Copyright (c) 2014 Midokura SARL, All Rights Reserved.
 */

package org.midonet.brain.services.vxgw;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.inject.Guice;
import com.google.inject.Injector;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.junit.Before;
import org.junit.Test;

import rx.Observable;
import rx.Subscription;

import org.midonet.brain.BrainTestUtils;
import org.midonet.brain.org.midonet.brain.test.RxTestUtils;
import org.midonet.brain.services.vxgw.monitor.HostMonitor;
import org.midonet.cluster.DataClient;
import org.midonet.cluster.EntityIdSetEvent;
import org.midonet.cluster.data.host.Host;
import org.midonet.midolman.serialization.SerializationException;
import org.midonet.midolman.state.Directory;
import org.midonet.midolman.state.StateAccessException;
import org.midonet.midolman.state.ZookeeperConnectionWatcher;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.midonet.cluster.EntityIdSetEvent.Type.CREATE;
import static org.midonet.cluster.EntityIdSetEvent.Type.DELETE;
import static org.midonet.cluster.EntityIdSetEvent.Type.STATE;

public class HostMonitorTest extends DeviceMonitorTestBase<UUID, Host> {

    /**
     * Midonet data client
     */
    private DataClient dataClient = null;
    private ZookeeperConnectionWatcher zkConnWatcher;

    private Host createHost(String name) throws SerializationException,
                                                StateAccessException {
        Host host = new Host();
        host.setId(UUID.randomUUID());
        host.setName(name);
        dataClient.hostsCreate(host.getId(), host);
        return host;
    }

    private RxTestUtils.TestedObservable testHostObservable(
        Observable<Host> obs) {
        return RxTestUtils.test(obs);
    }

    private RxTestUtils.TestedObservable testIdObservable(
        Observable<UUID> obs) {
        return RxTestUtils.test(obs);
    }

    private RxTestUtils.TestedObservable testEventObservable(
        Observable<EntityIdSetEvent<UUID>> obs) {
        return RxTestUtils.test(obs);
    }

    @Before
    public void before() throws Exception {
        HierarchicalConfiguration config = new HierarchicalConfiguration();
        BrainTestUtils.fillTestConfig(config);
        Injector injector = Guice.createInjector(
            BrainTestUtils.modules(config));

        Directory directory = injector.getInstance(Directory.class);
        BrainTestUtils.setupZkTestDirectory(directory);

        dataClient = injector.getInstance(DataClient.class);
        zkConnWatcher = new ZookeeperConnectionWatcher();
    }

    @Test
    public void testBasic() throws Exception {

        HostMonitor hMon = new HostMonitor(dataClient, zkConnWatcher);

        // Setup the observables
        RxTestUtils.TestedObservable updates =
            testHostObservable(hMon.getEntityObservable());
        updates.noElements().noErrors().notCompleted().subscribe();

        RxTestUtils.TestedObservable live =
            testEventObservable(hMon.getEntityIdSetObservable());
        live.noElements().noErrors().notCompleted().subscribe();

        RxTestUtils.TestedObservable creations =
            testIdObservable(extractEvent(hMon.getEntityIdSetObservable(),
                                          CREATE));
        creations.noElements().noErrors().notCompleted().subscribe();

        RxTestUtils.TestedObservable deletions =
            testIdObservable(extractEvent(hMon.getEntityIdSetObservable(),
                                          DELETE));
        deletions.noElements().noErrors().notCompleted().subscribe();

        updates.unsubscribe();
        live.unsubscribe();
        creations.unsubscribe();
        deletions.unsubscribe();

        updates.evaluate();
        live.evaluate();
        creations.evaluate();
        deletions.evaluate();
    }

    @Test
    public void testHostAddition() throws Exception {

        final List<UUID> creationList = new ArrayList<>();
        final List<UUID> updateList = new ArrayList<>();

        HostMonitor hMon = new HostMonitor(dataClient, zkConnWatcher);

        RxTestUtils.TestedObservable deletions =
            testIdObservable(extractEvent(hMon.getEntityIdSetObservable(), DELETE));
        deletions.noElements().noErrors().notCompleted().subscribe();

        Subscription creations = addIdObservableToList(
            extractEvent(hMon.getEntityIdSetObservable(), CREATE), creationList);
        Subscription updates = addDeviceObservableToList(
            hMon.getEntityObservable(), updateList);

        // Create the host
        Host host = createHost("host1");

        creations.unsubscribe();
        updates.unsubscribe();
        deletions.unsubscribe();

        assertThat(creationList, containsInAnyOrder(host.getId()));
        assertThat(updateList, containsInAnyOrder(host.getId()));
        deletions.evaluate();
    }

    @Test
    public void testHostEarlyAddition() throws Exception {

        final List<UUID> creationList = new ArrayList<>();
        final List<UUID> updateList = new ArrayList<>();
        final List<UUID> stateList = new ArrayList<>();

        // Create the host
        Host host = createHost("host1");

        HostMonitor hMon = new HostMonitor(dataClient, zkConnWatcher);

        RxTestUtils.TestedObservable deletions =
            testIdObservable(extractEvent(hMon.getEntityIdSetObservable(),
                                          DELETE));
        deletions.noElements().noErrors().notCompleted().subscribe();

        Subscription creations = addIdObservableToList(
            extractEvent(hMon.getEntityIdSetObservable(), CREATE),
            creationList);

        Subscription updates = addDeviceObservableToList(
            hMon.getEntityObservable(), updateList);

        Subscription states = addIdObservableToList(
            extractEvent(hMon.getEntityIdSetObservable(), STATE), stateList);

        hMon.notifyState();

        creations.unsubscribe();
        updates.unsubscribe();
        deletions.unsubscribe();
        states.unsubscribe();

        assertThat(creationList, containsInAnyOrder());
        assertThat(updateList, containsInAnyOrder(host.getId()));
        assertThat(stateList, containsInAnyOrder(host.getId()));
        deletions.evaluate();
    }

    @Test
    public void testHostRemoval() throws Exception {

        final List<UUID> creationList = new ArrayList<>();
        final List<UUID> updateList = new ArrayList<>();
        final List<UUID> deletionList = new ArrayList<>();

        HostMonitor hMon = new HostMonitor(dataClient, zkConnWatcher);

        Subscription creations = addIdObservableToList(
            extractEvent(hMon.getEntityIdSetObservable(), CREATE), creationList);
        Subscription updates = addDeviceObservableToList(
            hMon.getEntityObservable(), updateList);
        Subscription deletions = addIdObservableToList(
            extractEvent(hMon.getEntityIdSetObservable(), DELETE), deletionList);

        // Create the host
        Host host = createHost("host1");

        dataClient.hostsDelete(host.getId());

        creations.unsubscribe();
        updates.unsubscribe();
        deletions.unsubscribe();

        assertThat(creationList, containsInAnyOrder(host.getId()));
        assertThat(updateList, containsInAnyOrder(host.getId()));
        assertThat(deletionList, containsInAnyOrder(host.getId()));
    }
}
