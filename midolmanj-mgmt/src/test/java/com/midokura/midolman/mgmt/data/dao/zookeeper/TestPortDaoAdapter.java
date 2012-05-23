/*
 * Copyright 2012 Midokura KK
 * Copyright 2012 Midokura PTE LTD.
 */
package com.midokura.midolman.mgmt.data.dao.zookeeper;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.UUID;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.midokura.midolman.mgmt.data.dao.BgpDao;
import com.midokura.midolman.mgmt.data.dao.VpnDao;
import com.midokura.midolman.mgmt.data.dto.LogicalRouterPort;
import com.midokura.midolman.mgmt.data.dto.MaterializedRouterPort;
import com.midokura.midolman.mgmt.data.dto.Port;
import com.midokura.midolman.mgmt.data.zookeeper.op.PortOpService;

@RunWith(MockitoJUnitRunner.class)
public class TestPortDaoAdapter {

    private PortDaoAdapter testObject;

    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private PortZkDao dao;

    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private PortOpService opService;

    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private BgpDao bgpDao;

    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private VpnDao vpnDao;

    private static Port createTestMaterializedPort(UUID id, UUID routerId,
            UUID vifId) {
        MaterializedRouterPort port = new MaterializedRouterPort();
        port.setId(id);
        port.setDeviceId(routerId);
        port.setVifId(vifId);
        port.setLocalNetworkAddress("192.168.100.2");
        port.setLocalNetworkLength(24);
        port.setNetworkAddress("192.168.100.0");
        port.setNetworkLength(24);
        port.setPortAddress("192.168.100.1");
        return port;
    }

    private static Port createTestLogicalPort(UUID id, UUID routerId,
            UUID peerId, UUID peerRouterId) {
        LogicalRouterPort port = new LogicalRouterPort();
        port.setId(id);
        port.setDeviceId(routerId);
        port.setNetworkAddress("192.168.100.0");
        port.setNetworkLength(24);
        port.setPeerId(peerId);
        port.setPeerPortAddress("196.168.200.2");
        port.setPeerRouterId(peerRouterId);
        port.setPortAddress("192.168.200.1");
        return port;
    }

    @Before
    public void setUp() throws Exception {
        testObject = spy(new PortDaoAdapter(dao, opService, bgpDao, vpnDao));
    }

    @Test
    public void testGetNotExist() throws Exception {
        UUID id = UUID.randomUUID();
        doReturn(false).when(dao).exists(id);

        Port port = testObject.get(id);

        Assert.assertNull(port);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDeleteWithVifPluggedError() throws Exception {

        Port port = createTestMaterializedPort(UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID());
        doReturn(port).when(testObject).get(port.getId());

        testObject.delete(port.getId());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDeleteLogicalPortError() throws Exception {

        Port port = createTestLogicalPort(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID());
        doReturn(port).when(testObject).get(port.getId());

        testObject.delete(port.getId());
    }
}
