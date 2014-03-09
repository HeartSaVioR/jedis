package redis.clients.jedis.tests;

import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.exceptions.JedisAskDataException;
import redis.clients.jedis.exceptions.JedisClusterException;
import redis.clients.jedis.exceptions.JedisClusterMaxRedirectionsException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.exceptions.JedisMovedDataException;
import redis.clients.util.JedisClusterCRC16;

public class JedisClusterTest extends Assert {
    private Jedis node1;
    private static Jedis node2;
    private static Jedis node3;
    private static Jedis node4;

    private HostAndPort nodeInfo1 = HostAndPortUtil.getClusterServers().get(0);
    private HostAndPort nodeInfo2 = HostAndPortUtil.getClusterServers().get(1);
    private HostAndPort nodeInfo3 = HostAndPortUtil.getClusterServers().get(2);
    private HostAndPort nodeInfo4 = HostAndPortUtil.getClusterServers().get(3);
    
    @Before
    public void setUp() throws InterruptedException {
	node1 = new Jedis(nodeInfo1.getHost(), nodeInfo1.getPort());
	node1.connect();
	node1.flushAll();

	node2 = new Jedis(nodeInfo2.getHost(), nodeInfo2.getPort());
	node2.connect();
	node2.flushAll();

	node3 = new Jedis(nodeInfo3.getHost(), nodeInfo3.getPort());
	node3.connect();
	node3.flushAll();
	
	node4 = new Jedis(nodeInfo4.getHost(), nodeInfo4.getPort());
	node4.connect();
	node4.flushAll();

	// ---- configure cluster

	// add nodes to cluster
	node1.clusterMeet("127.0.0.1", nodeInfo2.getPort());
	node1.clusterMeet("127.0.0.1", nodeInfo3.getPort());

	// split available slots across the three nodes
	int slotsPerNode = JedisCluster.HASHSLOTS / 3;
	int[] node1Slots = new int[slotsPerNode];
	int[] node2Slots = new int[slotsPerNode+1];
	int[] node3Slots = new int[slotsPerNode];
	for (int i = 0, slot1 = 0, slot2 = 0, slot3 = 0 ; i < JedisCluster.HASHSLOTS; i++) {
	    if (i < slotsPerNode) {
		node1Slots[slot1++] = i;
	    } else if (i > slotsPerNode * 2) {
		node3Slots[slot3++] = i;
	    } else {
		node2Slots[slot2++] = i;
	    }
	}
	
	node1.clusterAddSlots(node1Slots);
	node2.clusterAddSlots(node2Slots);
	node3.clusterAddSlots(node3Slots);
	
	waitForClusterReady();
    }
    
    @AfterClass
    public static void cleanUp() {
	int slotTest = JedisClusterCRC16.getSlot("test");
	int slot51 = JedisClusterCRC16.getSlot("51");
	String node3Id = getNodeId(node3.clusterNodes());
	node2.clusterSetSlotNode(slotTest, node3Id);
	node2.clusterSetSlotNode(slot51, node3Id);
	node2.clusterDelSlots(slotTest, slot51);
    }

    @After
    public void tearDown() throws InterruptedException {
	// clear all slots
	int[] slotsToDelete = new int[JedisCluster.HASHSLOTS];
	for (int i = 0; i < JedisCluster.HASHSLOTS; i++) {
	    slotsToDelete[i] = i;
	}
	
	node1.clusterDelSlots(slotsToDelete);
	node2.clusterDelSlots(slotsToDelete);
	node3.clusterDelSlots(slotsToDelete);
    }

    @Test(expected = JedisMovedDataException.class)
    public void testThrowMovedException() {
	node1.set("foo", "bar");
    }

    @Test
    public void testMovedExceptionParameters() {
	try {
	    node1.set("foo", "bar");
	} catch (JedisMovedDataException jme) {
	    assertEquals(12182, jme.getSlot());
	    assertEquals(new HostAndPort("127.0.0.1", 7381),
		    jme.getTargetNode());
	    return;
	}
	fail();
    }

    @Test(expected = JedisAskDataException.class)
    public void testThrowAskException() {
	int keySlot = JedisClusterCRC16.getSlot("test");
	String node3Id = getNodeId(node3.clusterNodes());
	node2.clusterSetSlotMigrating(keySlot, node3Id);
	node2.get("test");
    }

    @Test
    public void testDiscoverNodesAutomatically() {
	Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
	jedisClusterNode.add(new HostAndPort("127.0.0.1", 7379));
	JedisCluster jc = new JedisCluster(jedisClusterNode);
	assertEquals(jc.getClusterNodes().size(), 3);
    }

    @Test
    public void testCalculateConnectionPerSlot() {
	Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
	jedisClusterNode.add(new HostAndPort("127.0.0.1", 7379));
	JedisCluster jc = new JedisCluster(jedisClusterNode);
	jc.set("foo", "bar");
	jc.set("test", "test");
	assertEquals("bar", node3.get("foo"));
	assertEquals("test", node2.get("test"));
    }

    @Test
    public void testRecalculateSlotsWhenMoved() throws InterruptedException {
	Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
	jedisClusterNode.add(new HostAndPort("127.0.0.1", 7379));
	JedisCluster jc = new JedisCluster(jedisClusterNode);
	int slot51 = JedisClusterCRC16.getSlot("51");
	node2.clusterDelSlots(slot51);
	node3.clusterDelSlots(slot51);
	node3.clusterAddSlots(slot51);

	waitForClusterReady();
	jc.set("51", "foo");
	assertEquals("foo", jc.get("51"));
    }

    @Test
    public void testAskResponse() throws InterruptedException {
	Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
	jedisClusterNode.add(new HostAndPort("127.0.0.1", 7379));
	JedisCluster jc = new JedisCluster(jedisClusterNode);
	int slot51 = JedisClusterCRC16.getSlot("51");
	node3.clusterSetSlotImporting(slot51, getNodeId(node2.clusterNodes()));
	node2.clusterSetSlotMigrating(slot51, getNodeId(node3.clusterNodes()));
	jc.set("51", "foo");
	assertEquals("foo", jc.get("51"));
    }

    @Test(expected = JedisClusterException.class)
    public void testThrowExceptionWithoutKey() {
	Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
	jedisClusterNode.add(new HostAndPort("127.0.0.1", 7379));
	JedisCluster jc = new JedisCluster(jedisClusterNode);
	jc.ping();
    }

    @Test(expected = JedisClusterMaxRedirectionsException.class)
    public void testRedisClusterMaxRedirections() {
	Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
	jedisClusterNode.add(new HostAndPort("127.0.0.1", 7379));
	JedisCluster jc = new JedisCluster(jedisClusterNode);
	int slot51 = JedisClusterCRC16.getSlot("51");
	// This will cause an infinite redirection loop
	node2.clusterSetSlotMigrating(slot51, getNodeId(node3.clusterNodes()));
	jc.set("51", "foo");
    }
    
    @Test
    public void testRedisHashtag() {
	assertEquals(JedisClusterCRC16.getSlot("{bar"), JedisClusterCRC16.getSlot("foo{{bar}}zap"));
	assertEquals(JedisClusterCRC16.getSlot("{user1000}.following"), JedisClusterCRC16.getSlot("{user1000}.followers"));
	assertNotEquals(JedisClusterCRC16.getSlot("foo{}{bar}"), JedisClusterCRC16.getSlot("bar"));
	assertEquals(JedisClusterCRC16.getSlot("foo{bar}{zap}"), JedisClusterCRC16.getSlot("bar"));
    }

    @Test
    public void testClusterForgetNode() throws InterruptedException {
	// at first, join node4 to cluster
	node1.clusterMeet("127.0.0.1", nodeInfo4.getPort());
	
	String node4Id = getNodeId(node4.clusterNodes());
	
	assertNodeIsKnown(node3, node4Id, 1000);
	assertNodeIsKnown(node2, node4Id, 1000);
	assertNodeIsKnown(node1, node4Id, 1000);
	
	assertEquals(4, node1.clusterNodes().split("\n").length);
	assertEquals(4, node2.clusterNodes().split("\n").length);
	assertEquals(4, node3.clusterNodes().split("\n").length);
	
	// do cluster forget
        node1.clusterForget(node4Id);
        node2.clusterForget(node4Id);
        node3.clusterForget(node4Id);
        
        assertNodeIsUnknown(node1, node4Id, 1000);
        assertNodeIsUnknown(node2, node4Id, 1000);
        assertNodeIsUnknown(node3, node4Id, 1000);
        
        assertEquals(3, node1.clusterNodes().split("\n").length);
        assertEquals(3, node2.clusterNodes().split("\n").length);
        assertEquals(3, node3.clusterNodes().split("\n").length);
    }
    
    @Test
    public void testClusterFlushSlots() {
	String slotRange = getNodeServingSlotRange(node1.clusterNodes()); 
	assertNotNull(slotRange);
	
	try {
	    node1.clusterFlushSlots();
	    assertNull(getNodeServingSlotRange(node1.clusterNodes()));
	} finally {
	    // rollback
	    String[] rangeInfo = slotRange.split("-");
	    int lower = Integer.parseInt(rangeInfo[0]);
	    int upper = Integer.parseInt(rangeInfo[1]);
	    
	    // FIXME: make it faster
	    for ( ; lower <= upper ; lower++) {
		node1.clusterAddSlots(lower);
	    }
	}
    }
    
    @Test
    public void testClusterKeySlot() {
	// It assumes JedisClusterCRC16 is correctly implemented
	assertEquals(node1.clusterKeySlot("foo{bar}zap}").intValue(), JedisClusterCRC16.getSlot("foo{bar}zap"));
	assertEquals(node1.clusterKeySlot("{user1000}.following").intValue(), JedisClusterCRC16.getSlot("{user1000}.following"));
    }
    
    private static String getNodeId(String infoOutput) {
	for (String infoLine : infoOutput.split("\n")) {
	    if (infoLine.contains("myself")) {
		return infoLine.split(" ")[0];
	    }
	}
	return "";
    }
    
    private static String getNodeServingSlotRange(String infoOutput) {
	// f4f3dc4befda352a4e0beccf29f5e8828438705d 127.0.0.1:7380 master - 0 1394372400827 0 connected 5461-10922
	for (String infoLine : infoOutput.split("\n")) {
	    if (infoLine.contains("myself")) {
		try {
		    return infoLine.split(" ")[8];
		} catch (ArrayIndexOutOfBoundsException e) {
		    return null;
		}
	    }
	}
	return null;
    }

    private void waitForClusterReady() throws InterruptedException {
	boolean clusterOk = false;
	while (!clusterOk) {
	    if (node1.clusterInfo().split("\n")[0].contains("ok")
		    && node2.clusterInfo().split("\n")[0].contains("ok")
		    && node3.clusterInfo().split("\n")[0].contains("ok")) {
		clusterOk = true;
	    }
	    Thread.sleep(50);
	}
    }
    
    private void assertNodeIsKnown(Jedis node, String targetNodeId, int timeoutMs) {
	assertNodeRecognizedStatus(node, targetNodeId, true, timeoutMs);
    }

    private void assertNodeIsUnknown(Jedis node, String targetNodeId, int timeoutMs) {
	assertNodeRecognizedStatus(node, targetNodeId, false, timeoutMs);
    }
    
    private void assertNodeRecognizedStatus(Jedis node, String targetNodeId, boolean shouldRecognized, int timeoutMs) {
	int sleepInterval = 100;
	for (int sleepTime = 0 ; sleepTime <= timeoutMs ; sleepTime += sleepInterval) {
	    boolean known = isKnownNode(node, targetNodeId);
	    if (shouldRecognized == known)
		return;
	    
	    try {
		Thread.sleep(sleepInterval);
	    } catch (InterruptedException e) {
	    }
	}
	
	throw new JedisException("Node recognize check error");

    }
    
    private static boolean isKnownNode(Jedis node, String nodeId) {
	String infoOutput = node.clusterNodes();
	for (String infoLine : infoOutput.split("\n")) {
	    if (infoLine.contains(nodeId)) {
		return true;
	    }
	}
	return false;
    }
    
}
