package com.fujitsu.us.oovn.core;

import org.junit.Test;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.hamcrest.CoreMatchers.is;

import com.fujitsu.us.oovn.exception.InvalidVNOOperationException;

public class VNOTest
{

    /**
     * Test the state transition of a VNO
     * Any invalid operation should trigger a InvalidVNOOperationException
     */
    @Test
    public void testState() throws InvalidVNOOperationException
    {
        // unconfigured
        VNO vno = new VNO(new Tenant("Carl"));
        assertThat(vno.getState(), is(VNO.VNOState.UNCONFIGURED));
        
        // should init first
        try
        {
            vno.verify();
            fail("Expected an InvalidVNOOperationException to be thrown");
        } catch (InvalidVNOOperationException e) {
            assertThat(e.getMessage(), is("The VNO is not initialized (configured) yet"));
        }
        
        // failed init
        vno.init("NoSuchFile.json");
        assertThat(vno.getState(), is(VNO.VNOState.UNCONFIGURED));
        
        // successful init
        vno.init("VirtualConfig1.json");
        assertThat(vno.getState(), is(VNO.VNOState.UNVERIFIED));
        
        // should verify() before activate()
        try
        {
            vno.activate();
            fail("Expected an InvalidVNOOperationException to be thrown");
        } catch (InvalidVNOOperationException e) {
            assertThat(e.getMessage(), is("The VNO is not verified yet"));
        }
        
        // should pass verification
        vno.verify();
        assertThat(vno.getState(), is(VNO.VNOState.INACTIVE));
        
        // cannot deactivate a non-active VNO
        try
        {
            vno.deactivate();
            fail("Expected an InvalidVNOOperationException to be thrown");
        } catch (InvalidVNOOperationException e) {
            assertThat(e.getMessage(), is("The VNO is not activated yet"));
        }
        
        vno.activate();
        assertThat(vno.getState(), is(VNO.VNOState.ACTIVE));
        
        vno.deactivate();
        assertThat(vno.getState(), is(VNO.VNOState.INACTIVE));
        
        vno.decommission();
        assertThat(vno.getState(), is(VNO.VNOState.DECOMMISSIONED));
    }
    
}