//HMS_UI.java
package org.fog.test.perfeval;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

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
import org.fog.placement.ModulePlacementOnlyCloud;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;


public class HMS_UI {
	
	static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
	static List<Sensor> sensors = new ArrayList<Sensor>();
	static List<Actuator> actuators = new ArrayList<Actuator>();
	
	
	static int numOfGateways = 2;
	static int numOfMobilesPerArea = 2;
	static int SheduleMethod;
	
	private static boolean CLOUD = false;
	
	public static void main(String[] args) {
		
	      JFrame frame = new JFrame("HMS_UI");
	      
	      Container cp = frame.getContentPane();
	      cp.setLayout(new FlowLayout());
	      
	      JLabel jl = new JLabel("Health Monitoring Applications  on IoT System");
	      jl.setBounds(20, 50, 120, 80);
	      cp.add(jl);

	      jl = new JLabel("No of Gateways:");
	      jl.setBounds(20, 80, 120, 80);
	      cp.add(jl);
	      
	      JTextField fd = new JTextField("2", 5);
	      fd.setHorizontalAlignment(JTextField.RIGHT);
	      fd.setBounds(50, 100, 120, 80);
	      cp.add(fd);

	      jl = new JLabel("No of Edge Devises per Gateway:");
	      jl.setBounds(80, 50, 120, 80);
	      cp.add(jl);

	      JTextField ed = new JTextField("2", 5);
	      ed.setHorizontalAlignment(JTextField.RIGHT);
	      ed.setBounds(80, 100, 120, 80);
	      cp.add(ed);

	      jl = new JLabel("Algorithem:");
	      jl.setBounds(110, 100, 100, 80);
	      cp.add(jl);

	      final String[] steps = {"CLOUD","PSO_LAT","PSO_POWER"};  // auto-upcast	      
	      final JComboBox <String> cAlg = new JComboBox<String>(steps);
	      cAlg.setPreferredSize(new Dimension(150, 20));
	      cp.add(cAlg);
	      
	      cAlg.addItemListener(new ItemListener() {
	         @Override
	         public void itemStateChanged(ItemEvent e) {
	            if (e.getStateChange() == ItemEvent.SELECTED) {
	               String s = (String) cAlg.getSelectedItem();
	               System.out.println("==Alg=="+s);
	            }
	         }
	      });
	      
	      JButton bRun = new JButton("<Run Semiulation>");
	      bRun.setPreferredSize(new Dimension(300, 40));
	      bRun.addActionListener(new ActionListener () {
	    	  public void actionPerformed(ActionEvent ae) {
	    		       		    
	    		        System.out.println("Button pressed!");
	    		        numOfGateways = Integer.valueOf(fd.getText());
	    		        numOfMobilesPerArea = Integer.valueOf(ed.getText());
	    		        String s = (String) cAlg.getSelectedItem();
	    		        if (s.equals("CLOUD"))  SheduleMethod = 0;
	    		        else if (s.equals("PSO_LAT")) SheduleMethod = 1;
	    		        else if (s.equals("PSO_POWER")) SheduleMethod = 2;    		        
//	    		        System.out.println("FR:"+numFogResources+" ER:"+numOfEdgeDvises + " Algorithem:"+SheduleMethod);
	    		        RunSimulation();
	    		    }   	  
	      });
	      cp.add(bRun);
	      frame.setResizable(false);
	      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	      frame.setSize(500, 500);  // or pack() the components
	      frame.setLocationRelativeTo(null);  // center the application window
	      frame.setVisible(true);             // show it

	}
	public static void	RunSimulation() {
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
			
			System.out.println("==Devices==");
			for(FogDevice device : fogDevices) {
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
						
			Controller controller = null;
			
			ModuleMapping moduleMapping = ModuleMapping.createModuleMapping(); // initializing a module mapping
						
			controller = new Controller("master-controller", fogDevices, sensors, 
					actuators);
			
			switch ( SheduleMethod ) {
				case 0:
					controller.submitApplication(application, new ModulePlacementOnlyCloud(fogDevices, sensors, actuators, application));
					break;
				case 1:
					controller.submitApplication(application, new ModulePlacementPSOBase(fogDevices, sensors, actuators, application, moduleMapping));
					break;
				case 2:
					controller.submitApplication(application, new ModulePlacementPSOLatPow(fogDevices, sensors, actuators, application, moduleMapping));
					break;
			}
  		
			
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
		
		FogDevice proxy = createFogDevice("proxy-server", 28000, 20000, 10000, 10000, 1, 0.0, 107.339, 83.4333);
		proxy.setParentId(cloud.getId());
		proxy.setUplinkLatency(200); // latency of connection between proxy server and cloud is 100 ms
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

		FogDevice mobile = createFogDevice("M-"+id, 1000, 1000, 10000, 10000, 3, 0, 87.53, 82.44);		
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
		application.addAppModule("CLIENT", 10);
		application.addAppModule("DATA_FILTER", 10);
		application.addAppModule("DATA_PROCESSING", 10);
		application.addAppModule("EVENT_HANDLER", 10);
		
		/*
		 * Connecting the application modules (vertices) in the application model (directed graph) with edges
		 */
		application.addAppEdge("SENSOR", "CLIENT", 1000, 500, "SENSOR", Tuple.UP, AppEdge.SENSOR);
		application.addAppEdge("CLIENT", "DATA_FILTER", 1000, 500, "RAW_DATA", Tuple.UP, AppEdge.MODULE); 
		application.addAppEdge("DATA_FILTER", "DATA_PROCESSING", 1000, 500, "FILTERED_DATA", Tuple.UP, AppEdge.MODULE); 
		application.addAppEdge("DATA_PROCESSING", "EVENT_HANDLER", 1000, 500, "PROCESSED_DATA", Tuple.UP, AppEdge.MODULE);
		application.addAppEdge("EVENT_HANDLER", "CLIENT", 1000, 500, "RESPONSE", Tuple.DOWN, AppEdge.MODULE); 
		application.addAppEdge("CLIENT", "DISPLAY", 0, 500, "PTZ_PARM", Tuple.ACTUATOR, AppEdge.ACTUATOR); 
		
		/*
		 * Defining the input-output relationships (represented by selectivity) of the application modules. 
		 */
		application.addTupleMapping("CLIENT", "SENSOR", "RAW_DATA", new FractionalSelectivity(1.0)); 
		application.addTupleMapping("CLIENT", "RESPONSE", "PTZ_PARM", new FractionalSelectivity(1.0)); 
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
