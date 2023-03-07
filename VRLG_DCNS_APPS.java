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
import org.fog.placement.ModulePlacementMapping;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;

public class VRLG_DCNS_APPS {
	static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
	static List<FogDevice> mobiles = new ArrayList<FogDevice>();
	static List<Sensor> sensors = new ArrayList<Sensor>();
	static List<Actuator> actuators = new ArrayList<Actuator>();
	
	static int numFogResources = 2;
	static int numOfEdgeDvises = 4;
	static double EEG_TRANSMISSION_TIME = 5;
	//static double EEG_TRANSMISSION_TIME = 10;
	
	public static void main(String[] args) {

		Log.printLine("Starting VRLG_DCNS_APPS together...");

		try {
			Log.disable();
			int num_user = 1; // number of cloud users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false; // mean trace events

			CloudSim.init(num_user, calendar, trace_flag);

			String appId0 = "VRGAME";
			String appId1 = "DCNS";
			
			FogBroker broker0 = new FogBroker("broker_0");
			FogBroker broker1 = new FogBroker("broker_1");
			
			
			Application application0 = createApplication0(appId0, broker0.getId());
			Application application1 = createApplication1(appId1, broker1.getId());
			application0.setUserId(broker0.getId());
			application1.setUserId(broker1.getId());
			
			createIoTNetworktopology();			
			createEdgeDevicesVRG(broker0.getId(), appId0);
			createEdgeDevicesDCNS(broker1.getId(), appId1);
			
			System.out.println("==Devices==");
			for(FogDevice device : fogDevices) {
				System.out.println(device.getName() +" (" + device.getHost().getTotalMips() + ")");				
			}
			System.out.println("==Mobiles==(same mobile are also in the list of Devises)");
			for(FogDevice device : mobiles) {
				System.out.println(device.getName() +" (" + device.getHost().getTotalMips() + ")");				
			}
			System.out.println("==Sensors==");
			for(Sensor device : sensors) {
				System.out.println(device.getName());				
			}
			System.out.println("==Actuators==");
			for(Actuator device : actuators) {
				System.out.println(device.getName());				
			}
			
			
			ModuleMapping moduleMapping_0 = ModuleMapping.createModuleMapping(); // initializing a module mapping
			ModuleMapping moduleMapping_1 = ModuleMapping.createModuleMapping(); // initializing a module mapping
			
			moduleMapping_0.addModuleToDevice("coordinator", "cloud"); // fixing all instances of the Connector module to the Cloud
			moduleMapping_1.addModuleToDevice("user_interface", "cloud"); // fixing all instances of the Connector module to the Cloud

			for(FogDevice device : fogDevices){
				if(device.getName().startsWith("m-V")){
					moduleMapping_0.addModuleToDevice("client", device.getName());  // fixing all instances of the Client module to the Smartphones
					// fixing all instances of the Client module to the Smartphones
				}
				if(device.getName().startsWith("m-D")){
					moduleMapping_1.addModuleToDevice("motion_detector", device.getName());  
				}
				if(device.getName().startsWith("d")){
				moduleMapping_0.addModuleToDevice("concentration_calculator", device.getName()); // fixing all instances of the Concentration Calculator module to the Cloud
				moduleMapping_1.addModuleToDevice("object_detector", device.getName()); // placing all instances of Object Detector module in the Cloud
				moduleMapping_1.addModuleToDevice("object_tracker", device.getName()); // placing all instances of Object Tracker module in the Cloud
				}
			}
			
			Controller controller = new Controller("master-controller", fogDevices, sensors, 
					actuators);
			
			controller.submitApplication(application0, new ModulePlacementMapping(fogDevices, application0, moduleMapping_0));
			controller.submitApplication(application1, new ModulePlacementMapping(fogDevices, application1, moduleMapping_1));

			TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());

			CloudSim.startSimulation();

			CloudSim.stopSimulation();

			Log.printLine("simulation finished!");
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Unwanted errors happen");
		}
	}

	private static void createEdgeDevicesVRG(int userId, String appId) {
		for(FogDevice mobile : mobiles){
			String id = mobile.getName();
			if(id.startsWith("m-VRLG")) {
				Sensor eegSensor = new Sensor("s-"+id, "EEG", userId, appId, new DeterministicDistribution(EEG_TRANSMISSION_TIME)); // inter-transmission time of EEG sensor follows a deterministic distribution
				sensors.add(eegSensor);
				Actuator display = new Actuator("a-"+id, userId, appId, "DISPLAY");
				actuators.add(display);
				eegSensor.setGatewayDeviceId(mobile.getId());
				eegSensor.setLatency(1.0);  // latency of connection between EEG sensors and the parent Smartphone is 6 ms
				display.setGatewayDeviceId(mobile.getId());
				display.setLatency(1.0);  // latency of connection between Display actuator and the parent Smartphone is 1 ms	
			}
		}
	}
	
	private static void createEdgeDevicesDCNS(int userId, String appId) {
		for(FogDevice mobile : mobiles){
			String id = mobile.getName();
			if(id.startsWith("m-DCNS")) {
				Sensor sensor = new Sensor("s-"+id, "CAMERA", userId, appId, new DeterministicDistribution(5)); // inter-transmission time of camera (sensor) follows a deterministic distribution
				sensors.add(sensor);
				Actuator ptz = new Actuator("ptz-"+id, userId, appId, "PTZ_CONTROL");
				actuators.add(ptz);
				sensor.setGatewayDeviceId(mobile.getId());
				sensor.setLatency(1.0);  // latency of connection between camera (sensor) and the parent Smart Camera is 1 ms
				ptz.setGatewayDeviceId(mobile.getId());
				ptz.setLatency(1.0);  // latency of connection between PTZ Control and the parent Smart Camera is 1 ms
			}	
		}
	}

	/**
	 * Creates the fog devices in the physical topology of the simulation.
	 * @param userId
	 * @param appId
	 */
	private static void createIoTNetworktopology() {
		FogDevice cloud = createFogDevice("cloud", 44800, 40000, 100, 10000, 0, 0.01, 16*103, 16*83.25); // creates the fog device Cloud at the apex of the hierarchy with level=0
		cloud.setParentId(-1);
		FogDevice gateway = createFogDevice("d-RSFC-Gateway", 2800, 4000, 10000, 10000, 1, 0.0, 107.339, 83.4333); // creates the fog device Proxy Server (level=1)
		gateway.setParentId(cloud.getId()); // setting Cloud as parent of the Proxy Server
		gateway.setUplinkLatency(100); // latency of connection from Proxy Server to the Cloud is 100 ms
		
		fogDevices.add(cloud);
		fogDevices.add(gateway);
		
		for(int i=0;i<numFogResources;i++){
			addFogResourcess("R:"+i, gateway.getId()); // adding a fog device for every Gateway in physical topology. The parent of each gateway is the Proxy Server
		}	
		
		for(int i=0;i<numOfEdgeDvises;i++){
			addEdgeResourcess("VRLG:"+i, gateway.getId()); // adding a fog device for every Gateway in physical topology. The parent of each gateway is the Proxy Server
		}	
		
		for(int i=0;i<numOfEdgeDvises;i++){
			addEdgeResourcess("DCNS:"+i, gateway.getId()); // adding a fog device for every Gateway in physical topology. The parent of each gateway is the Proxy Server
		}	
	}

	private static FogDevice addFogResourcess(String id, int parentId){
		FogDevice fogdevice = createFogDevice("d-"+id, 2800, 4000, 10000, 10000, 1, 0.0, 107.339, 83.4333);
		fogDevices.add(fogdevice);
		fogdevice.setParentId(parentId);
		fogdevice.setUplinkLatency(0); // latency of connection between gateways and proxy server is 1 ms
		return fogdevice;
	}	
	
	private static FogDevice addEdgeResourcess (String id, int parentId) {
		
		FogDevice mobile = createFogDevice("m-"+id, 1000, 1000, 10000, 270, 3, 0, 87.53, 82.44);
		mobile.setParentId(parentId);
		mobiles.add(mobile);
		mobile.setUplinkLatency(4); // latency of connection between the smartphone and proxy server is 4 ms
		fogDevices.add(mobile);
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
	 * Function to create the EEG Tractor Beam game application in the DDF model. 
	 * @param appId unique identifier of the application
	 * @param userId identifier of the user of the application
	 * @return
	 */
	@SuppressWarnings({"serial" })
	private static Application createApplication0(String appId, int userId){
		
		Application application = Application.createApplication(appId, userId); // creates an empty application model (empty directed graph)
		
		/*
		 * Adding modules (vertices) to the application model (directed graph)
		 */
		application.addAppModule("client", 10); // adding module Client to the application model
		application.addAppModule("concentration_calculator", 10); // adding module Concentration Calculator to the application model
		application.addAppModule("coordinator", 10); // adding module Connector to the application model
		
		/*
		 * Connecting the application modules (vertices) in the application model (directed graph) with edges
		 */
		application.addAppEdge("EEG", "client", 1000, 500, "EEG", Tuple.UP, AppEdge.SENSOR);
		application.addAppEdge("client", "concentration_calculator", 1000, 500, "_SENSOR", Tuple.UP, AppEdge.MODULE); // adding edge from Client to Concentration Calculator module carrying tuples of type _SENSOR
		application.addAppEdge("concentration_calculator", "coordinator", 100, 500, 500, "PLAYER_GAME_STATE", Tuple.UP, AppEdge.MODULE); // adding periodic edge (period=1000ms) from Concentration Calculator to Connector module carrying tuples of type PLAYER_GAME_STATE
		application.addAppEdge("concentration_calculator", "client", 20, 500, "CONCENTRATION", Tuple.DOWN, AppEdge.MODULE);  // adding edge from Concentration Calculator to Client module carrying tuples of type CONCENTRATION
		application.addAppEdge("coordinator", "client", 100, 28, 1000, "GLOBAL_GAME_STATE", Tuple.DOWN, AppEdge.MODULE); // adding periodic edge (period=1000ms) from Connector to Client module carrying tuples of type GLOBAL_GAME_STATE
		application.addAppEdge("client", "DISPLAY", 500, 500, "SELF_STATE_UPDATE", Tuple.DOWN, AppEdge.ACTUATOR);  // adding edge from Client module to Display (actuator) carrying tuples of type SELF_STATE_UPDATE
		application.addAppEdge("client", "DISPLAY", 500, 500, "GLOBAL_STATE_UPDATE", Tuple.DOWN, AppEdge.ACTUATOR);  // adding edge from Client module to Display (actuator) carrying tuples of type GLOBAL_STATE_UPDATE
		
		/*
		 * Defining the input-output relationships (represented by selectivity) of the application modules. 
		 */
		application.addTupleMapping("client", "EEG", "_SENSOR", new FractionalSelectivity(0.9)); // 0.9 tuples of type _SENSOR are emitted by Client module per incoming tuple of type EEG 
		application.addTupleMapping("client", "CONCENTRATION", "SELF_STATE_UPDATE", new FractionalSelectivity(1.0)); // 1.0 tuples of type SELF_STATE_UPDATE are emitted by Client module per incoming tuple of type CONCENTRATION 
		application.addTupleMapping("client", "GLOBAL_GAME_STATE", "GLOBAL_STATE_UPDATE", new FractionalSelectivity(1.0)); // 1.0 tuples of type GLOBAL_STATE_UPDATE are emitted by Client module per incoming tuple of type GLOBAL_GAME_STATE 

		application.addTupleMapping("concentration_calculator", "_SENSOR", "CONCENTRATION", new FractionalSelectivity(1.0)); // 1.0 tuples of type CONCENTRATION are emitted by Concentration Calculator module per incoming tuple of type _SENSOR 
		application.addTupleMapping("concentration_calculator", "_SENSOR", "PLAYER_GAME_STATE", new FractionalSelectivity(1.0)); // 1.0 tuples of type GLOBAL_STATE_UPDATE are emitted by Client module per incoming tuple of type GLOBAL_GAME_STATE 

		application.addTupleMapping("coordinator", "PLAYER_GAME_STATE", "GLOBAL_GAME_STATE", new FractionalSelectivity(1.0)); // 1.0 tuples of type CONCENTRATION are emitted by Concentration Calculator module per incoming tuple of type _SENSOR 

		/*
		 * Defining application loops to monitor the latency of. 
		 * Here, we add only one loop for monitoring : EEG(sensor) -> Client -> Concentration Calculator -> Client -> DISPLAY (actuator)
		 */
		final AppLoop loop1 = new AppLoop(new ArrayList<String>(){{add("EEG");add("client");add("concentration_calculator");add("client");add("DISPLAY");}});
		List<AppLoop> loops = new ArrayList<AppLoop>(){{add(loop1);}};
		application.setLoops(loops);
		
		return application;
	}
	
	@SuppressWarnings({"serial" })
	private static Application createApplication1(String appId, int userId){
		
		Application application = Application.createApplication(appId, userId); // creates an empty application model (empty directed graph)
		
		/*
		 * Adding modules (vertices) to the application model (directed graph)
		 */
		application.addAppModule("user_interface", 10); 
		application.addAppModule("object_detector", 10); 
		application.addAppModule("object_tracker", 10); 
		application.addAppModule("motion_detector", 10); 	
		/*
		 * Connecting the application modules (vertices) in the application model (directed graph) with edges
		 */
		application.addAppEdge("CAMERA", "motion_detector", 1000, 500, "CAMERA", Tuple.UP, AppEdge.SENSOR); // adding edge from EEG (sensor) to Client module carrying tuples of type EEG
		application.addAppEdge("motion_detector", "object_detector", 1000, 500, "_SENSOR_1", Tuple.UP, AppEdge.MODULE); // adding edge from Client to Concentration Calculator module carrying tuples of type _SENSOR
		application.addAppEdge("object_detector", "object_tracker", 100, 500, 500, "OBJID", Tuple.UP, AppEdge.MODULE); // adding periodic edge (period=1000ms) from Concentration Calculator to Connector module carrying tuples of type PLAYER_GAME_STATE
		application.addAppEdge("object_tracker", "user_interface", 100, 500, "DISPLAY1", Tuple.UP, AppEdge.MODULE);  // adding edge from Concentration Calculator to Client module carrying tuples of type CONCENTRATION
		application.addAppEdge("object_tracker", "PTZ_CONTROL", 100, 500, "PTZ_PARAM", Tuple.DOWN, AppEdge.ACTUATOR);  // adding edge from Client module to Display (actuator) carrying tuples of type SELF_STATE_UPDATE
		
		/*
		 * Defining the input-output relationships (represented by selectivity) of the application modules. 
		 */
		application.addTupleMapping("motion_detector", "CAMERA", "_SENSOR_1", new FractionalSelectivity(0.9)); // 0.9 tuples of type _SENSOR are emitted by Client module per incoming tuple of type EEG 
		application.addTupleMapping("object_detector", "_SENSOR_1", "OBJID", new FractionalSelectivity(1.0)); // 1.0 tuples of type SELF_STATE_UPDATE are emitted by Client module per incoming tuple of type CONCENTRATION 
		application.addTupleMapping("object_tracker", "OBJID", "PTZ_PARAM", new FractionalSelectivity(1.0)); // 1.0 tuples of type CONCENTRATION are emitted by Concentration Calculator module per incoming tuple of type _SENSOR 
		application.addTupleMapping("user_interface", "OBJID", "GLOBAL_DISPLY", new FractionalSelectivity(1.0)); // 1.0 tuples of type GLOBAL_STATE_UPDATE are emitted by Client module per incoming tuple of type GLOBAL_GAME_STATE 
	
		/*
		 * Defining application loops to monitor the latency of. 
		 * Here, we add only one loop for monitoring : EEG(sensor) -> Client -> Concentration Calculator -> Client -> DISPLAY (actuator)
		 */
		final AppLoop loop1 = new AppLoop(new ArrayList<String>(){{add("CAMERA");add("motion_detector");add("object_detector");add("object_tracker");add("PTZ_CONTROL");}});
		List<AppLoop> loops = new ArrayList<AppLoop>(){{add(loop1);}};
		application.setLoops(loops);
		
		return application;
	}
}