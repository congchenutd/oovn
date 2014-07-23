package com.fujitsu.us.oovn.element.link;

import java.util.LinkedList;
import java.util.List;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.Node;

import com.fujitsu.us.oovn.core.VNO;
import com.fujitsu.us.oovn.element.Persistable;
import com.fujitsu.us.oovn.element.datapath.VirtualSwitch;
import com.fujitsu.us.oovn.element.port.VirtualPort;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class VirtualLink extends Link<VirtualSwitch, VirtualPort> implements Persistable
{
    private final VNO          _vno;
    private List<PhysicalLink> _path;
    
    public VirtualLink(VNO vno, VirtualPort src, VirtualPort dst)
    {
        super(src, dst);
        _vno  = vno;
        _path = new LinkedList<PhysicalLink>();
    }
    
    public void setPath(List<PhysicalLink> path)
    {
        if(path == null || path.isEmpty())
            return;
        
        // the ends of the path should match those of the virtual link
        if(path.get(0)            .getSrcPort().equals(getSrcPort().getPhysicalPort()) && 
           path.get(path.size()-1).getDstPort().equals(getDstPort().getPhysicalPort()))
           _path = path;
    }
    
    public List<PhysicalLink> getPath() {
        return _path;
    }
    
    public VNO getVNO() {
        return _vno;
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if(this == obj)
            return true;
        
        if(!super.equals(obj))
            return false;
        
        if(!(obj instanceof VirtualLink))
            return false;
        
        VirtualLink that = (VirtualLink) obj;
        return  this.getVNO() == null && that.getVNO() == null ||
                this.getVNO() != null && that.getVNO() != null && 
                this.getVNO().equals(that.getVNO());
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("VirtualLink: ");
        for(PhysicalLink link: getPath())
            builder.append(link.toDBVariable() + ",");
        return builder.toString();
    }
    
    @Override
    public JsonElement toJson()
    {
        JsonObject result = (JsonObject) super.toJson();
        if(!getPath().isEmpty())
        {
            JsonArray path = new JsonArray();
            for(PhysicalLink link: getPath())
                path.add(link.toJson());
            result.add("path", path);
        }
        return result;
    }
    
    @Override
    public String toDBMatch() {
        return "(" + toDBVariable() + 
                ":Virtual:Link " + "{" + 
                    "vnoid:" + getVNO().getID() + "," +
                    "srcSwitch:" + "\"" + getSrcSwitch().getDPID().toString() + "\", " +
                    "srcPort:" + getSrcPort().getNumber() + "," +
                    "dstSwitch:" + "\"" + getDstSwitch().getDPID().toString() + "\", " +
                    "dstPort:" + getDstPort().getNumber() +
                "})";
    }

    @Override
    public void createMapping(ExecutionEngine engine)
    {
        if(getPath().isEmpty())
            return;
        
        StringBuilder builder = new StringBuilder();
        builder.append("MATCH \n");
        for(PhysicalLink link: getPath())
            builder.append(link.toDBMatch() + ",\n");
        builder.append(toDBMatch() + "\n");
        builder.append("CREATE \n");
        int order = 1;
        for(PhysicalLink link: getPath())
            builder.append("(" + toDBVariable() + ")-[:Maps {order:" + order++ + "}]->(" + 
                           link.toDBVariable() + "),");
        builder.deleteCharAt(builder.length() - 1);  // remove last comma
        engine.execute(builder.toString());    
    }
    
    /**
     * Find the virtual link in the VNO from a db node
     * @param node  a Neo4j node representing a virtual link
     * @param vno
     * @return      the corresponding VirtualLink object
     */
    public static VirtualLink fromNode(Node node, VNO vno)
    {
        String srcDPID = node.getProperty("srcSwitch").toString();
        String dstDPID = node.getProperty("dstSwitch").toString();
        int  srcNumber = Integer.valueOf(node.getProperty("srcPort").toString());
        int  dstNumber = Integer.valueOf(node.getProperty("dstPort").toString());
        return vno.getNetwork().getLink(srcDPID, srcNumber, dstDPID, dstNumber);
    }
    
}
