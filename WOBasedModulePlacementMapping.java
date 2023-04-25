//WOBasedModulePlacementMapping.java

package org.fog.test.perfeval;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacement;
import org.fog.utils.TimeKeeper;

class Whale {
	
	AppLoop apploop;
	List<FogDevice>  no_devises;
	List<AppModule> no_modules;
	List <AppEdge> listedges; 
	int MaxDeviceLeve;
	
	double fitness ;
	
	int[][] indextble = new int[8][8];
	
	Whale(List<FogDevice> fds, List<AppModule> mdls, AppLoop apploop, List <AppEdge> listedges) {
		this.no_devises =fds ;  
		this.no_modules =mdls; 
		this.apploop = apploop;
		this.listedges = listedges;
		this.fitness =0 ;
		this.MaxDeviceLeve = 0;
		for(FogDevice device : no_devises){
			if (device.getLevel() > this.MaxDeviceLeve) {
				this.MaxDeviceLeve = device.getLevel();
			}
		}
	}

	int wGet(int i,int j) {
		return indextble[i][j];
	}
	
	void RandumInitialize() {		
		for ( int i = 0 ;i< MaxDeviceLeve+1; i++)
		     for ( int j = 0 ;j< no_modules.size();  j++) {
		    	 indextble[i][j] =0;

		     }
		Random rand = new Random();
		
		for (int i = 0; i< no_modules.size(); i++) {
	        int r = rand.nextInt(MaxDeviceLeve+1);		
	        indextble[r][i]++;
		}
		ComputeFitness();
	}
	
	void Copy(Whale w) {		
		for ( int i = 0 ;i< MaxDeviceLeve+1; i++)
		     for ( int j = 0 ;j< no_modules.size();  j++) {
		    	 indextble[i][j] =w.indextble[i][j];

		     }
		ComputeFitness();
	}
	
	
	
	double ComputeFitness() {
		
		double loopdelay = 0.0;
		List <String> loop_modules = apploop.getModules();	
		System.out.println(loop_modules);
		
		for (int i =0; i < loop_modules.size()-1; i++) {

			for(AppEdge appedge : listedges){
				if (appedge.getSource().equalsIgnoreCase(loop_modules.get(i)) && appedge.getDestination().equalsIgnoreCase(loop_modules.get(i+1))) {
					double tplCpuLenth = appedge.getTupleCpuLength();
					double tplNwLenth= appedge.getTupleNwLength();
//					System.out.println("Source:"+appedge.getSource()+ " Distination:"+appedge.getDestination() + " CPU ln:" +tplCpuLenth + " NW L"+ tplNwLenth);	
					
					double latncy = 0;
					double cpuMips =0;

					if (appedge.getEdgeType() == AppEdge.ACTUATOR) {
						latncy =1;
						latncy = latncy + GetLatencySourcToDest(loop_modules.get(i),null);
						loopdelay = loopdelay + latncy*tplNwLenth  ;
//						System.out.println("latncy:"+latncy +" tplNwLenth:"+tplNwLenth);	
//						System.out.println(loopdelay);

					} else if (appedge.getEdgeType() == AppEdge.SENSOR) {
						latncy = 6;		//assuming SENSOR is at mobile devise(MAX_LEVE -1)				
						latncy = latncy + GetLatencySourcToDest(null,loop_modules.get(i+1));
						cpuMips = GetModuleMapedResoureMIPS(appedge.getDestination());	
						loopdelay = loopdelay + latncy*tplNwLenth + tplCpuLenth /cpuMips*1000 ;					
//						System.out.println("latncy:"+latncy +" tplNwLenth:"+tplNwLenth+" NoOfInstructions:"+ tplCpuLenth + " Speed(MilionIPS):"+cpuMips );	
//						System.out.println(loopdelay);

					} else {
						cpuMips = GetModuleMapedResoureMIPS(appedge.getDestination());	
						latncy = GetLatencySourcToDest(loop_modules.get(i),loop_modules.get(i+1));
						loopdelay = loopdelay + latncy*tplNwLenth + tplCpuLenth /cpuMips*1000  ;
//						System.out.println("latncy:"+latncy +" tplNwLenth:"+tplNwLenth+" NoOfInstructions:"+ tplCpuLenth + " Speed(MilionIPS):"+cpuMips );	
//						System.out.println(loopdelay);
						//latency in milliseconds, network_length in Bytes, CPU_length in bytes	,	CPU(speed) in Million Instruction Per Second
					}
					break;
				}
			}
		}
		System.out.println("Total loopdelay(seconds)"+loopdelay/1000);
		fitness = loopdelay/1000;
    	return fitness;	
	}
		
	private FogDevice GetModuleMapedResoure(String m) {
		FogDevice fd = null;		
//		List<AppModule> no_modules;
		int k = 0;
//		System.out.println(m);
		for (k = 0; k< no_modules.size(); k++) {
			if ( no_modules.get(k).getName().equalsIgnoreCase(m))
				break;  
		}
		
		int level = 0;
		for ( level = 0 ; level< MaxDeviceLeve+1; level++) {
		    	 if (indextble[level][k] == 1) 
		    		 break;
		}		
		
		for(FogDevice device : no_devises){
			if (device.getLevel() == level) {
				fd = device;
				break;
			}
		}
		
		return fd;
	}

	private double GetLatencySourcToDest(String source, String dest) {
		// TODO Auto-generated method stub
		
		FogDevice srcDev = null, destDev = null;
		
		double latency = 0.0;
		int srcL =0,destL=0;
		//if source is null meens it is SENSER so we set srcL to MAXLEVEL
		if (source != null) {
			srcDev = GetModuleMapedResoure(source);	
			srcL= srcDev.getLevel();
		} else srcL = MaxDeviceLeve;
		
		//if dest is null meens it is ACTUATER so we set destL to MAXLEVEL
		if (dest != null) {
			destDev = GetModuleMapedResoure(dest);
			destL=destDev.getLevel();
		} else destL = MaxDeviceLeve;
		
		
		while (srcL != destL ) {
			
			if (destL > srcL) {
					srcL++ ;
					for(FogDevice device : no_devises){
						if (device.getLevel() == srcL) {
							srcDev = device;
							break;
						}
					}
					latency = latency + srcDev.getUplinkLatency();
				}
			if (destL < srcL) {		
					for(FogDevice device : no_devises){
						if (device.getLevel() == srcL) {
							srcDev = device;
							latency = latency + srcDev.getUplinkLatency();							
							break;
						}
					}
					srcL-- ;
				}
		}

		return latency;
	}
	
	private int GetModuleMapedResoureMIPS(String m) {
		// TODO Auto-generated method stub
		int cpuMips = 0;		
//		List<AppModule> no_modules;
		int k = 0;
		for (k = 0; k< no_modules.size(); k++) {
//			System.out.println(m + " compare-K"+ k + no_modules.get(k).getName());			
			if ( no_modules.get(k).getName().equalsIgnoreCase(m))
				break;  
		}
		int level = 0;
		for ( level = 0 ; level<  MaxDeviceLeve+1; level++) {
		    	 if (indextble[level][k] == 1) 
		    		 break;
		}
		
		for(FogDevice device : no_devises){
			if (device.getLevel() == level) {
				cpuMips = device.getHost().getTotalMips();
				break;
			}
		}
		return cpuMips;
	}
	
	void printpWhaleMap() {
		for ( int i = 0 ;i<  MaxDeviceLeve+1;i++) {
			 if (i==0)
			 { System.out.print("       ");
				 for ( int j = 0 ;j< no_modules.size();  j++) {
					 System.out.print(no_modules.get(j).getName()+ "  ") ;
				 }
				 System.out.println();
			 }
		     System.out.print("Level("+i+"):  ");
		     for ( int j = 0 ;j< no_modules.size();  j++) {
		    	 System.out.print(indextble[i][j]+"        ");
		     }
		     System.out.println();
		}
		
	}
	
	//Rotate
	void Spiral(int var ) {
		//movement direct or spiral(rotate) attack - var is random number var is random no between (0-1)
		//if var < 0.5 attack in rotating(towords best) 
		//if vat >0.5 other wise direct -->in dirict we hav two options based on A
		//if A<1 -> dirict attack(Towards Best) else-> random Search
	}
	
	
	void MoveWhale(int dist , Whale toWhale ) {		
		if (toWhale != null) {
			for ( int i = 0 ;i<  MaxDeviceLeve+1;i++) {
			     for ( int j = 0 ;j< no_modules.size();  j++) {
			    	if  (dist == 0) break;    	
			    	if ( indextble[i][j] != toWhale.indextble[i][j]) {
			    		int temp =indextble[i][j];
			    		 //moved towords toWhale
			    		if (temp == 0) {
			    			for ( int k = 0 ; k< MaxDeviceLeve+1; k++) {
			    				indextble[k][j] = temp;
			    			}
			    		indextble[i][j] = toWhale.indextble[i][j];
			    		} else {   indextble[i][j] = toWhale.indextble[i][j];
				    		if (i+1 <  MaxDeviceLeve+1)
				    			indextble[i+1][j] = temp;
				    		else indextble[0][j] = temp;	

			    		}
			    		dist --;
			    	}
			     }
			}
		}
		//Compute Fitness for Updated-Whale
		ComputeFitness();
	}
}


public class WOBasedModulePlacementMapping extends ModulePlacement{

	private ModuleMapping moduleMapping;
	public static int MAX_NO_MODLS = 4;
	public static int MAX_NO_R_LEVELS = 4;
	public static int NO_WHALES = 10;
	public static int NO_ITERATIONS = 10;
	List <AppModule> apMdls;
	List <AppEdge> appedges; 
	List <AppLoop> apploops;
	Whale selected_whale;
	
	@Override

	protected void mapModules() {
			Map<String, List<String>> mapping = moduleMapping.getModuleMapping();
			for(String deviceName : mapping.keySet()){
				FogDevice device = getDeviceByName(deviceName);
				for(String moduleName : mapping.get(deviceName)){
					
					AppModule module = getApplication().getModuleByName(moduleName);
					if(module == null)
						continue;
					createModuleInstanceOnDevice(module, device);
					//getModuleInstanceCountMap().get(device.getId()).put(moduleName, mapping.get(deviceName).get(moduleName));
				}
			}
	}

	public WOBasedModulePlacementMapping(List<FogDevice> fogDevices, List<Sensor> sensors, List<Actuator> actuators, 
			Application application, ModuleMapping moduleMapping){
		this.setFogDevices(fogDevices);
		this.setApplication(application);
		this.setModuleMapping(moduleMapping);
		
		this.setModuleToDeviceMap(new HashMap<String, List<Integer>>());
		this.setDeviceToModuleMap(new HashMap<Integer, List<AppModule>>());
		this.setModuleInstanceCountMap(new HashMap<Integer, Map<String, Integer>>());
		

		
		for(FogDevice device : getFogDevices())
			getModuleInstanceCountMap().put(device.getId(), new HashMap<String, Integer>());
		
		WOResourceSheduleing();
		
		mapModules();
	}
	
	private void WOResourceSheduleing() {
		// TODO Auto-generated method stub

		this.apMdls = getApplication().getModules();
		this.appedges = getApplication().getEdges();
		this.apploops = getApplication().getLoops();
		
		AppLoop apploop = apploops.get(0);
		
		List <Whale> whales = new LinkedList<>(); 
		
		for (int i = 0 ; i <NO_WHALES; i++) {
			Whale w = new Whale(getFogDevices(),apMdls, apploop, appedges);
			w.RandumInitialize();
			whales.add(w);
		}		

//WO Algorithm	loop	
		 Whale gBest_whalel = whales.get(0);
		double gBest = gBest_whalel.fitness;
		
		double A = 2.0;
		double step_a = 2.0/NO_ITERATIONS;
		double a = 0;

		for (int iCont = 0 ; iCont <NO_ITERATIONS; iCont++) {
		
//updating better solution
			for (int i = 0 ; i <whales.size(); i++) {
					Whale w = whales.get(i);
					
					if ( gBest > w.fitness) {
						gBest_whalel = w;
						gBest = w.fitness;			
					}
			}
								
				//move each  to new position with speed and diractionTowards				
			for (int i = 0 ; i <whales.size(); i++) {
				 Whale w = whales.get(i);
				 
				// update Whale-logic goes here
					Random rand = new Random();
					
					float p = rand.nextFloat(1);
					System.out.println("p:"+p + "Iteration:"+iCont);
					if (p < 0.5) {
						
							if (a<1) {	
							//update to current best -->attack
								w.Copy(gBest_whalel);
								
							} else {
							//Random move in search space
								w.RandumInitialize();
							}	

					} else {
						//spiral rotate (whale is change by distance 2 towards Best)
						w.MoveWhale(2, gBest_whalel);
					} 
			}	
		}
	
		selected_whale = gBest_whalel;	
		
		System.out.println("Fitness Value:"+selected_whale.fitness);		
		selected_whale.printpWhaleMap();

		int mdL = 0;
		for(FogDevice device : getFogDevices()){
			if (device.getLevel() > mdL) {
				mdL = device.getLevel();
			}
		}
//mdL will have no module in the system
		
		for (int i = 0 ;i < mdL+1; i++) {
			for(FogDevice device : fogDevices){
				if ( device.getLevel()==i) {
					for (int j = 0 ; j < apMdls.size(); j++) {
						if (1==selected_whale.wGet(i, j)) {
								moduleMapping.addModuleToDevice(apMdls.get(j).getName(), device.getName());
								
						}
					}
				}
			}
		}
		UpdateAppEdgesAccordingtoModuleMaping();
	}
	
	void UpdateAppEdgesAccordingtoModuleMaping() {
		for(AppEdge appedge : appedges) {			
			if ( AppEdge.MODULE == appedge.getEdgeType()) {
				int mdlSorceId = 0;
				int mdlDestId = 0;
				int srcL = 0;
				int destL=0;
				int direction =  Tuple.UP;
				for (int j = 0 ; j < apMdls.size(); j++) {
					if (apMdls.get(j).getName().equalsIgnoreCase(appedge.getSource())) mdlSorceId = j;
					if (apMdls.get(j).getName().equalsIgnoreCase(appedge.getDestination()))mdlDestId = j ;
				}
// find the level of source model and level of dest model, based on that initialise direction
				
				for (int j = 0 ; j < apMdls.size(); j++) {
					if (1==selected_whale.wGet(j, mdlSorceId)) srcL = j;
					if (1==selected_whale.wGet(j, mdlDestId)) destL=j;
				}				
				
				if (srcL < destL) direction = Tuple.DOWN;
				else direction = Tuple.UP;
				
				appedge.setDirection(direction);				
				
				System.out.println("Direction:" +appedge.getDirection());				
			}
		}
	}
	
	public ModuleMapping getModuleMapping() {
		return moduleMapping;
	}
	
	public void setModuleMapping(ModuleMapping moduleMapping) {
		this.moduleMapping = moduleMapping;
	}
}
