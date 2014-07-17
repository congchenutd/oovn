package com.fujitsu.us.oovn.element.port;

import com.fujitsu.us.oovn.element.Persistable;
import com.fujitsu.us.oovn.element.address.MACAddress;
import com.fujitsu.us.oovn.element.datapath.PhysicalSwitch;
import com.fujitsu.us.oovn.element.link.PhysicalLink;

public class PhysicalPort extends Port<PhysicalSwitch, PhysicalLink> implements Persistable
{
    public PhysicalPort(int number, MACAddress mac)
    {
        super(number, mac);
    }
    
    @Override
    public String toDBCreate() {
        return  "(" + getName() +
                ":Physical:Port {" +
                "switch:\"" + getSwitch().getDPID().toString() + "\"," +
                "number:" + getNumber() + "," +
                "mac:\""  + getMACAddress() + "\"})";
    }
    
    @Override
    public String toDBMatch() {
        return  toDBCreate();
    }
    
    @Override
    public String toDBMapping() {
        return null;
    }

}