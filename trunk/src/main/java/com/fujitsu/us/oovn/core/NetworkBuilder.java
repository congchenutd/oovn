package com.fujitsu.us.oovn.core;

import com.fujitsu.us.oovn.element.address.DPID;
import com.fujitsu.us.oovn.element.address.IPAddress;
import com.fujitsu.us.oovn.element.datapath.BigSwitch;
import com.fujitsu.us.oovn.element.datapath.PhysicalSwitch;
import com.fujitsu.us.oovn.element.datapath.SingleSwitch;
import com.fujitsu.us.oovn.element.datapath.VirtualSwitch;
import com.fujitsu.us.oovn.element.host.Host;
import com.fujitsu.us.oovn.element.link.LinkPair;
import com.fujitsu.us.oovn.element.link.VirtualLink;
import com.fujitsu.us.oovn.element.network.PhysicalNetwork;
import com.fujitsu.us.oovn.element.network.VirtualNetwork;
import com.fujitsu.us.oovn.element.port.PhysicalPort;
import com.fujitsu.us.oovn.element.port.VirtualPort;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class NetworkBuilder
{
    // singleton
    public static NetworkBuilder getInstance() {
        return LazyHolder._instance;
    }
    
    private static class LazyHolder {
        private static final NetworkBuilder _instance = new NetworkBuilder();
    }
    
    private NetworkBuilder() {}

    public VirtualNetwork build(VNO vno)
    {
        JsonObject json = vno.getConfiguration().toJson();
        JsonObject vnoJson = json.getAsJsonObject("vno");
        
        // global
        String address = vnoJson.get("address").getAsString();
        int    mask    = vnoJson.get("mask")   .getAsInt();
        VirtualNetwork result = new VirtualNetwork(vno, new IPAddress(address), mask);
        
        // switches
        JsonArray switchesJson = vnoJson.getAsJsonArray("switches");
        for(JsonElement e: switchesJson)
        {
            JsonObject swJson = (JsonObject) e;
            VirtualSwitch sw = buildSwitch(swJson);
            result.addSwitch(sw);
        }
        
        // links
        JsonArray linksJson = vnoJson.getAsJsonArray("links");
        for(JsonElement e: linksJson)
        {
            JsonObject linkPairJson = (JsonObject) e;
            LinkPair linkPair = buildLinkPair(linkPairJson);
            result.addLinkPair(linkPair);
        }
        
        // hosts
        JsonArray hostsJson = vnoJson.getAsJsonArray("hosts");
        for(JsonElement e: hostsJson)
        {
            JsonObject hostJson = (JsonObject) e;
            Host host = buildHost(hostJson);
            result.addHost(host);
        }
        
        return result;
    }

    private SingleSwitch buildSingleSwitch(JsonObject json)
    {
        DPID dpid   = new DPID(json.get("dpid").getAsString());
        String name = json.get("name").getAsString();
        SingleSwitch result = new SingleSwitch(dpid, name);
        
        // map to physical switch
        DPID phyID = new DPID(json.get("physical").getAsString());
        PhysicalSwitch sw = (PhysicalSwitch) PhysicalNetwork.getInstance().getSwitch(phyID);
        result.setPhysicalSwitch(sw);
        
        // virtual ports
        JsonArray portsJson = json.get("ports").getAsJsonArray();
        
        int index = 0;
        for(JsonElement e: portsJson)
        {
            int number = e.getAsInt();
            PhysicalPort port = (PhysicalPort) sw.getPort(number);
            VirtualPort vPort = new VirtualPort(++index, port.getMACAddress());
            vPort.setPhysicalPort(port);
            result.addPort(vPort);
        }
        
        return result;
    }
    
    private BigSwitch buildBigSwitch(JsonObject json)
    {
        return null;
    }
    
    private VirtualSwitch buildSwitch(JsonObject json) {
        return json.get("type").getAsString().equals("single") ? buildSingleSwitch(json) 
                                                               : buildBigSwitch(json);
    }
    
    private VirtualPort buildPort(JsonObject json)
    {
        Long dpid   = json.get("dpid")  .getAsLong();
        int  number = json.get("number").getAsInt();
        return null;
    }
    
    private LinkPair buildLinkPair(JsonObject json)
    {
        JsonObject egressJson  = json.getAsJsonObject("egress");
        JsonObject ingressJson = json.getAsJsonObject("ingress");
        return new LinkPair(buildLink(ingressJson), buildLink(egressJson));
    }
    
    private VirtualLink buildLink(JsonObject json)
    {
        if(json.isJsonNull())
            return null;
        
        return null;
    }
    
    private Host buildHost(JsonObject json)
    {
        int id = json.get("id").getAsInt();
        String name = json.get("name").getAsString();
        json.get("mac").getAsString();
        return null;
    }
    
}
