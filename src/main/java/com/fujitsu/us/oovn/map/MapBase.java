package com.fujitsu.us.oovn.map;

import java.util.LinkedList;
import java.util.List;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import com.fujitsu.us.oovn.core.VNO;
import com.fujitsu.us.oovn.element.address.PhysicalIPAddress;
import com.fujitsu.us.oovn.element.address.VirtualIPAddress;
import com.fujitsu.us.oovn.element.datapath.PhysicalSwitch;
import com.fujitsu.us.oovn.element.datapath.VirtualSwitch;
import com.fujitsu.us.oovn.element.link.PhysicalLink;
import com.fujitsu.us.oovn.element.link.VirtualLink;
import com.fujitsu.us.oovn.element.network.PhysicalNetwork;
import com.fujitsu.us.oovn.element.port.PhysicalPort;
import com.fujitsu.us.oovn.element.port.VirtualPort;

/**
 * Base class for all the maps
 * 
 * Internally a Neo4j graph holding all the mapping information
 * All query operations are implemented using Neo4j queries
 * 
 * @author Cong Chen <Cong.Chen@us.fujitsu.com>
 */
public class MapBase
{
    
    /**
     * @param vsw a VirtualSwitch
     * @return a list of PhysicalSwitches vsw maps to
     */
    public List<PhysicalSwitch> getPhysicalSwitches(VirtualSwitch vsw)
    {
        try(Transaction tx = _graphDb.beginTx())
        {
            ExecutionResult result = 
                _engine.execute("MATCH " + vsw.toDBMatch() + 
                                "-[:Maps]->(psw) " +
                                "RETURN psw");
            
            ResourceIterator<Node> it = result.columnAs("psw");
            List<PhysicalSwitch> switches = new LinkedList<PhysicalSwitch>();
            while(it.hasNext())
                switches.add(PhysicalSwitch.fromNode(it.next()));

            return switches;
        }
    }
    
    /**
     * @param psw a PhysicalSwitch
     * @param vno a VNO
     * @return the VirtualSwitch of the vno that maps to maps to psw
     */
    public VirtualSwitch getVirtualSwitch(PhysicalSwitch psw, VNO vno)
    {
        try(Transaction tx = _graphDb.beginTx())
        {
            ExecutionResult result = 
                _engine.execute("MATCH " + psw.toDBMatch() + 
                                "<-[:Maps]-(vsw {vnoid:" + vno.getID() + "}) " +
                                "RETURN vsw");
            
            ResourceIterator<Node> it = result.columnAs("vsw");
            return it.hasNext() ? VirtualSwitch.fromNode(it.next(), vno)
                                : null;
        }
    }
    
    /**
     * @param vPort a VirtualPort
     * @return the PhysicalPort vPort maps to
     */
    public PhysicalPort getPhysicalPort(VirtualPort vPort)
    {
        try(Transaction tx = _graphDb.beginTx())
        {
            ExecutionResult result = 
                _engine.execute("MATCH " + vPort.toDBMatch() + 
                                "-[:Maps]->(pPort) " +
                                "RETURN pPort");
            
            ResourceIterator<Node> it = result.columnAs("pPort");
            return it.hasNext() ? PhysicalPort.fromNode(it.next())
                                : null;
        }
    }
    
    /**
     * @param pPort a PhysicalPort
     * @param vno a VNO
     * @return the VirtualPort of vno that maps to pPort
     */
    public VirtualPort getVirtualPort(PhysicalPort pPort, VNO vno)
    {
        try(Transaction tx = _graphDb.beginTx())
        {
            ExecutionResult result = 
                _engine.execute("MATCH " + pPort.toDBMatch() + 
                                "<-[:Maps]-(vPort {vnoid:" + vno.getID() + "}) " +
                                "RETURN vPort");
            
            ResourceIterator<Node> it = result.columnAs("vPort");
            return it.hasNext() ? VirtualPort.fromNode(it.next(), vno)
                                : null;
        }
    }
    
    /**
     * @param vLink a VirtualLink
     * @return a list of PhysicalLinks that vLink maps to
     */
    public List<PhysicalLink> getPhysicalLinks(VirtualLink vLink)
    {
        try(Transaction tx = _graphDb.beginTx())
        {
            ExecutionResult result = 
                _engine.execute("MATCH " + vLink.toDBMatch() + 
                                "-[m:Maps]->(pLink) " +
                                "RETURN pLink " +
                                "ORDER BY m.order");
            
            ResourceIterator<Node> it = result.columnAs("pLink");
            List<PhysicalLink> links = new LinkedList<PhysicalLink>();
            while(it.hasNext())
                links.add(PhysicalLink.fromNode(it.next()));

            return links;
        }
    }
    
    /**
     * @param pLink a PhysicalLink
     * @param vno   a VNO
     * @return the VirtualLink of the vno that maps to pLink
     */
    public VirtualLink getVirtualLink(PhysicalLink pLink, VNO vno)
    {
        try(Transaction tx = _graphDb.beginTx())
        {
            ExecutionResult result = 
                _engine.execute("MATCH " + pLink.toDBMatch() + 
                                "<-[:Maps]-(vLink {vnoid:" + vno.getID() + "}) " +
                                "RETURN vLink");
            
            ResourceIterator<Node> it = result.columnAs("vLink");
            return it.hasNext() ? VirtualLink.fromNode(it.next(), vno)
                                : null;
        }
    }
    
    public PhysicalIPAddress getPhysicalIPAddress(VirtualIPAddress vIP)
    {
        return null;
    }
    
    public VirtualIPAddress getVirtualIPAddress(PhysicalIPAddress pIP, VNO vno)
    {
        return null;
    }
    
    /**
     * XXX: ugly! Shouldn't expose the query engine. But Verifiers may need to query
     *      the db directly, much more efficient 
     * @return
     */
    public ExecutionEngine getExecutionEngine() {
        return _engine;
    }
    
    public ExecutionResult query(String query)
    {
        ExecutionResult result;
        try(Transaction tx = _graphDb.beginTx())
        {
            result = _engine.execute(query);
            tx.success();
        }
        return result;
    }
    
    /**
     * Add a VNO to the map 
     * @param vno
     */
    public void registerVNO(VNO vno)
    {
        if(!vno.isVerified())
            return;
        
        try(Transaction tx = _graphDb.beginTx())
        {
            vno.getNetwork().createInDB(_engine);
            tx.success();
        }
        
        deactivateVNO(vno);
    }
    
    /**
     * Remove a VNO from the map
     * @param vno
     */
    public void unregisterVNO(VNO vno)
    {
        try(Transaction tx = _graphDb.beginTx())
        {
            // remove all the virtual nodes with the given vno id
            _engine.execute("MATCH (n:Virtual {vnoid:" + vno.getID() +  
                            "}) OPTIONAL MATCH (n)-[r]-() DELETE n,r");
            tx.success();
        }
    }
    
    /**
     * Activate a VNO by setting the "activated" property of all the nodes to true
     */
    public void activateVNO(VNO vno)
    {
        try(Transaction tx = _graphDb.beginTx())
        {
            _engine.execute("MATCH (n:Virtual {vnoid:" + vno.getID() + "})" +
                            "SET n.activated=true");
            tx.success();
        }
    }
    
    /**
     * Deactivate a VNO by setting the "activated" property of all the nodes to false
     */
    public void deactivateVNO(VNO vno)
    {
        try(Transaction tx = _graphDb.beginTx())
        {
            _engine.execute("MATCH (n:Virtual {vnoid:" + vno.getID() + "})" +
                            "SET n.activated=false");
            tx.success();
        }
    }
    
    protected final GraphDatabaseService _graphDb;
    protected final ExecutionEngine      _engine;
    
    /**
     * Initiate the map from the PhysicalNetwork object
     */
    protected MapBase(String dbPath)
    {
        _graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(dbPath);
        _engine  = new ExecutionEngine(_graphDb);
        registerShutdownHook(_graphDb);
        
        try(Transaction tx = _graphDb.beginTx())
        {
            _engine.execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE n,r");
            PhysicalNetwork.getInstance().createInDB(_engine);
            tx.success();
        }
    }
    
    /**
     * Registers a shutdown hook for the Neo4j instance so that it shuts down nicely 
     * when the VM exits (even if you "Ctrl-C" the running application).
     */
    private static void registerShutdownHook(final GraphDatabaseService graphDb)
    {
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run() {
                graphDb.shutdown();
            }
        } );
    }

}

//class LoggingExecutionEngine extends ExecutionEngine
//{
//    private final Logger _logger;
//    
//    public LoggingExecutionEngine(GraphDatabaseService database, Logger logger)
//    {
//        super(database);
//        _logger = logger;
//    }
//    
//    @Override
//    public ExecutionResult execute(String query) throws CypherException
//    {
////        _logger.info(query);
//        System.out.println(query);
//        return super.execute(query);
//    }
//    
//}