package com.fujitsu.us.oovn.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Tenant
{
    private final int               _id;
    private final String            _name;
    private final Map<Integer, VNO> _vnos;
    
    public Tenant(String name)
    {
        _name = name;
        _id   = TenantCounter.getNextID();
        _vnos = new HashMap<Integer, VNO>();
    }
    
    public int getID() {
        return _id;
    }
    
    public String getName() {
        return _name;
    }
    
    public boolean registerVNO(VNO vno)
    {
        if(_vnos.containsKey(vno.getID()))
            return false;
        _vnos.put(vno.getID(), vno);
        return true;
    }
    
    public boolean unregisterVNO(VNO vno)
    {
        _vnos.remove(vno.getID());
        return true;
    }
    
    public Map<Integer, VNO> getVNOs() {
        return Collections.unmodifiableMap(_vnos);
    }
    
    public VNO getVNO(int id) {
        return _vnos.containsKey(id) ? _vnos.get(id) : null;
    }
    
    public static void main(String[] args)
    {
        Tenant tenant = new Tenant("Carl");
        VNO vno = new VNO(tenant);
        NetworkConfiguration topo = vno.getPhysicalTopology();
//        System.out.println(topo.toString());
        
        vno.init("VirtualConfig.json");
//        System.out.println(vno.getConfiguration().toString());
        
        if(vno.verify())
        {
            vno.activate();
            vno.deactivate();
            vno.decommission();
        }
    }
}

class TenantCounter
{
    private static int _counter = 0;
    
    public static int getNextID() {
        return ++ _counter;
    }
}