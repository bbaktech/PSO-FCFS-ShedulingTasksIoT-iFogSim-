package org.fog.test.perfeval;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacementEdgewards;
import org.fog.placement.ModulePlacementMapping;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;

/**
 * Simulation setup for case study 2 - Intelligent Surveillance
 * @author Harshit Gupta
 *
 */
public class HMS {
	
	static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
	static List<Sensor> sensors = new ArrayList<Sensor>();
	static List<Actuator> actuators = new ArrayList<Actuator>();
	
	
	static int numOfGateways = 2;
	static int numOfMobilesPerArea = 2;
	
	private static boolean CLOUD = false;
	
	public static void main(String[] args) {

		Log.printLine("Starting Health Monitoring System...");

		try {
			Log.disable();
			int num_user = 1; // number of cloud users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false; // mean trace events

			CloudSim.init(num_user, calendar, trace_flag);

			String appId = "HMS"; // identifier of the application
			
			FogBroker broker = new FogBroker("broker");
			
			Application application = createApplication(appId, broker.getId());
			application.setUserId(broker.getId());
			
			createFogDevices(broker.getId(), appId);
			
//			System.out.println("==Devices==");
//			for(FogDevice device : fogDevices) {
//				System.out.println(device.getName() +" (" + device.getHost().getTotalMips() + ")");				
//			}
//
//			System.out.println("==Sensors==");
//			for(Sensor device : sensors) {
//				System.out.println(device.getName());				
//			}
//			System.out.println("==Actuators==");
//			for(Actuator device : actuators) {
//				System.out.println(device.getName());				
//			}
						
			Controller controller = null;
			
			ModuleMapping moduleMapping = ModuleMapping.createModuleMapping(); // initializing a module mapping
						
			controller = new Controller("master-controller", fogDevices, sensors, 
					actuators);
			
			controller.submitApplication(application, new ModulePlacementPSOBase(fogDevices, sensors, actuators, application, moduleMapping));
			
			TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());
			
			CloudSim.startSimulation();

			CloudSim.stopSimulation();

			Log.printLine("HMS finished!");
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Unwanted errors happen");
		}
	}
	
	/**
	 * Creates the fog devices in the physical topology of the simulation.
	 * @param userId
	 * @param appId
	 */
	private static void createFogDevices(int userId, String appId) {
		FogDevice cloud = createFogDevice("cloud", 44800, 40000, 100, 10000, 0, 0.01, 16*103, 16*83.25);
		cloud.setParentId(-1);
		fogDevices.add(cloud);
		
		FogDevice proxy = createFogDevice("proxy-server", 2800, 4000, 10000, 10000, 1, 0.0, 107.339, 83.4333);
		proxy.setParentId(cloud.getId());
		proxy.setUplinkLatency(100); // latency of connection between proxy server and cloud is 100 ms
		fogDevices.add(proxy);
		
		for(int i=0; i<numOfGateways; i++){
			addGateWay(i+"", userId, appId, proxy.getId());
		}
	}

	private static FogDevice addGateWay(String id, int userId, String appId, int parentId){
		FogDevice router = createFogDevice("G-"+id, 2800, 4000, 10000, 10000, 2, 0.0, 107.339, 83.4333);
		fogDevices.add(router);		
		router.setUplinkLatency(4); // latency of connection between router and proxy server is 2 ms
		
		for(int i=0;i<numOfMobilesPerArea;i++){
			String mobileId = id+"-"+i;
			FogDevice mobile = addMobileDevise(mobileId, userId, appId, router.getId()); // adding a smart camera to the physical topology. Smart cameras have been modeled as fog devices as well.
			mobile.setUplinkLatency(2); // latency of connection between camera and router is 2 ms
			fogDevices.add(mobile);
		}
		router.setParentId(parentId);
		return router;
	}
	
	private static FogDevice addMobileDevise(String id, int userId, String appId, int parentId){

		FogDevice mobile = createFogDevice("M-"+id, 500, 1000, 10000, 10000, 3, 0, 87.53, 82.44);		
		mobile.setParentId(parentId);
		
		Sensor sensor = new Sensor("s-"+id, "SENSOR", userId, appId, new DeterministicDistribution(5)); // inter-transmission time of (sensor) follows a deterministic distribution
		sensors.add(sensor);
		
		Actuator ptz = new Actuator("ptz-"+id, userId, appId, "DISPLAY");
		actuators.add(ptz);
		
		sensor.setGatewayDeviceId(mobile.getId());
		sensor.setLatency(6.0);  // latency of connection between camera (sensor) and the parent Smart Camera is 1 ms
		
		ptz.setGatewayDeviceId(mobile.getId());
		ptz.setLatency(1.0);  // latency of connection between PTZ Control and the parent Smart Camera is 1 ms
		return mobile;
	}
	
	/**
	 * Creates a vanilla fog device
	 * @param nodeName name of the device to be used in simulation
	 * @param mips MIPS
	 * @param ram RAM
	 * @param upBw uplink bandwidth
	 * @param downBw downlink bandwidth
	 * @param level hierarchy level of the device
	 * @param ratePerMips cost rate per MIPS used
	 * @param busyPower
	 * @param idlePower
	 * @return
	 */
	private static FogDevice createFogDevice(String nodeName, long mips,
			int ram, long upBw, long downBw, int level, double ratePerMips, double busyPower, double idlePower) {
		
		List<Pe> peList = new ArrayList<Pe>();

		// 3. Create PEs and add these into a list.
		peList.add(new Pe(0, new PeProvisionerOverbooking(mips))); // need to store Pe id and MIPS Rating

		int hostId = FogUtils.generateEntityId();
		long storage = 1000000; // host storage
		int bw = 10000;

		PowerHost host = new PowerHost(
				hostId,
				new RamProvisionerSimple(ram),
				new BwProvisionerOverbooking(bw),
				storage,
				peList,
				new StreamOperatorScheduler(peList),
				new FogLinearPowerModel(busyPower, idlePower)
			);

		List<Host> hostList = new ArrayList<Host>();
		hostList.add(host);

		String arch = "x86"; // system architecture
		String os = "Linux"; // operating system
		String vmm = "Xen";
		double time_zone = 10.0; // time zone this resource located
		double cost = 3.0; // the cost of using processing in this resource
		double costPerMem = 0.05; // the cost of using memory in this resource
		double costPerStorage = 0.001; // the cost of using storage in this
										// resource
		double costPerBw = 0.0; // the cost of using bw in this resource
		LinkedList<Storage> storageList = new LinkedList<Storage>(); // we are not adding SAN
													// devices by now

		FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
				arch, os, vmm, host, time_zone, cost, costPerMem,
				costPerStorage, costPerBw);

		FogDevice fogdevice = null;
		try {
			fogdevice = new FogDevice(nodeName, characteristics, 
					new AppModuleAllocationPolicy(hostList), storageList, 10, upBw, downBw, 0, ratePerMips);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		fogdevice.setLevel(level);
		return fogdevice;
	}

	/**
	 * Function to create the Intelligent Surveillance application in the DDF model. 
	 * @param appId unique identifier of the application
	 * @param userId identifier of the user of the application
	 * @return
	 */
	@SuppressWarnings({"serial" })
	private static Application createApplication(String appId, int userId){
		
		Application application = Application.createApplication(appId, userId);
		/*
		 * Adding modules (vertices) to the application model (directed graph)
		 */
		application.addAppModule("CLIENT", 128);
		application.addAppModule("DATA_FILTER", 512);
		application.addAppModule("DATA_PROCESSING", 512);
		application.addAppModule("EVENT_HANDLER", 2048);
		
		/*
		 * Connecting the application modules (vertices) in the application model (directed graph) with edges
		 */
		application.addAppEdge("SENSOR", "CLIENT", 1000, 500, "SENSOR", Tuple.UP, AppEdge.SENSOR);
		application.addAppEdge("CLIENT", "DATA_FILTER", 2000, 500, "RAW_DATA", Tuple.UP, AppEdge.MODULE); 
		application.addAppEdge("DATA_FILTER", "DATA_PROCESSING", 2000, 500, "FILTERED_DATA", Tuple.UP, AppEdge.MODULE); 
		application.addAppEdge("DATA_PROCESSING", "EVENT_HANDLER", 4000, 500, "PROCESSED_DATA", Tuple.UP, AppEdge.MODULE);
		application.addAppEdge("EVENT_HANDLER", "CLIENT", 100, 500, "RESPONSE", Tuple.DOWN, AppEdge.MODULE); 
		application.addAppEdge("CLIENT", "DISPLAY", 0, 500, "PTZ_PARM", Tuple.ACTUATOR, AppEdge.ACTUATOR); 
		
		/*
		 * Defining the input-output relationships (represented by selectivity) of the application modules. 
		 */
		application.addTupleMapping("CLIENT", "SENSOR", "RAW_DATA", new FractionalSelectivity(0.9)); 
		application.addTupleMapping("CLIENT", "RESPONSE", "PTZ_PARM", new FractionalSelectivity(0.1)); 
		application.addTupleMapping("DATA_FILTER", "RAW_DATA", "FILTERED_DATA", new FractionalSelectivity(1.0)); 
		application.addTupleMapping("DATA_PROCESSING", "FILTERED_DATA", "PROCESSED_DATA", new FractionalSelectivity(1.0)); 
		application.addTupleMapping("EVENT_HANDLER", "PROCESSED_DATA", "RESPONSE", new FractionalSelectivity(1.0));
		
		/*
		 * Defining application loops (maybe incomplete loops) to monitor the latency of. 
		 * Here, we add two loops for monitoring : Motion Detector -> Object Detector -> Object Tracker and Object Tracker -> PTZ Control
		 */
		final AppLoop loop1 = new AppLoop(new ArrayList<String>(){ {add("SENSOR"); add("CLIENT"); add("DATA_FILTER"); add("DATA_PROCESSING"); add("EVENT_HANDLER"); add("CLIENT"); add("DISPLAY"); }});		
		List <AppLoop> loops = new ArrayList<AppLoop>(){
				{add(loop1);}
			};
		
		application.setLoops(loops);
		return application;
	}
}
