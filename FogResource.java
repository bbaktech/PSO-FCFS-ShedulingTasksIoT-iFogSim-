//FogResource.java

package org.fog.test.perfeval;
import java.util.*;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.application.AppModule;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Tuple;
import org.fog.utils.FogEvents;
import org.fog.utils.FogUtils;
import org.fog.utils.Logger;
import org.fog.utils.NetworkUsageMonitor;

public class FogResource extends FogDevice {
	
	public FogResource(
            String name,
            FogDeviceCharacteristics characteristics,
            VmAllocationPolicy vmAllocationPolicy,
            List<Storage> storageList,
            double schedulingInterval,
            double uplinkBandwidth, double downlinkBandwidth, double uplinkLatency, double ratePerMips)  throws Exception
	{
			
		super(name, characteristics, vmAllocationPolicy, storageList,
				schedulingInterval, uplinkBandwidth, downlinkBandwidth, uplinkLatency, ratePerMips);
		 
	}
	
	protected void executeTuple(SimEvent ev, String moduleName)
	{
		 super.executeTuple(ev, moduleName);
	}

	protected void updateAllocatedMips(String incomingOperator)
	{	        
	        super.updateAllocatedMips(incomingOperator);

	}
    protected void sendDownFreeLink(Tuple tuple, int childId) {
        double networkDelay = tuple.getCloudletFileSize() / getDownlinkBandwidth();
        //Logger.debug(getName(), "Sending tuple with tupleType = "+tuple.getTupleType()+" DOWN");
        setSouthLinkBusy(true);
        //System.out.println(getName()+" Sending tuple with tupleType = "+tuple.getTupleType()+" to "+childId);
        double latency = 4;
        send(getId(), networkDelay, FogEvents.UPDATE_SOUTH_TUPLE_QUEUE);
        send(childId, networkDelay + latency, FogEvents.TUPLE_ARRIVAL, tuple);
        NetworkUsageMonitor.sendingTuple(latency, tuple.getCloudletFileSize());
    }
	
	protected void checkCloudletCompletion()
	{
		super.checkCloudletCompletion();
	}
}