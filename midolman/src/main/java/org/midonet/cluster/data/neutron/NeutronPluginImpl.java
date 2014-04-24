/*
 * Copyright (c) 2014 Midokura Europe SARL, All Rights Reserved.
 */
package org.midonet.cluster.data.neutron;

import com.google.inject.Inject;
import org.apache.zookeeper.Op;
import org.midonet.cluster.LocalDataClientImpl;
import org.midonet.midolman.serialization.SerializationException;
import org.midonet.midolman.state.StateAccessException;
import org.midonet.midolman.state.zkManagers.BridgeZkManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * MidoNet implementation of Neutron plugin interface.
 */
@SuppressWarnings("unused")
public class NeutronPluginImpl extends LocalDataClientImpl
        implements NeutronPlugin {

    private final static Logger log =
            LoggerFactory.getLogger(NeutronPluginImpl.class);

    @Inject
    private NetworkZkManager networkZkManager;

    @Override
    public Network createNetwork(@Nonnull Network network)
            throws StateAccessException, SerializationException {

        List<Op> ops = new ArrayList<>();
        networkZkManager.prepareCreateNetwork(ops, network);
        commitOps(ops);

        return getNetwork(network.id);
    }

    @Override
    public void deleteNetwork(@Nonnull UUID id)
            throws StateAccessException, SerializationException {

        List<Op> ops = new ArrayList<>();
        networkZkManager.prepareDeleteNetwork(ops, id);
        commitOps(ops);
    }

    @Override
    public Network getNetwork(@Nonnull UUID id)
            throws StateAccessException, SerializationException {
        return networkZkManager.getNetwork(id);
    }

    @Override
    public List<Network> getNetworks()
            throws StateAccessException, SerializationException {
        return networkZkManager.getNetworks();
    }

    @Override
    public Network updateNetwork(@Nonnull UUID id, @Nonnull Network network)
            throws StateAccessException, SerializationException,
            BridgeZkManager.VxLanPortIdUpdateException {

        List<Op> ops = new ArrayList<>();
        networkZkManager.prepareUpdateNetwork(ops, network);

        // Throws NotStatePathException if it does not exist.
        commitOps(ops);

        return getNetwork(id);
    }

}
