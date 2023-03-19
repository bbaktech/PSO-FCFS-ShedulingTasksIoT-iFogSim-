//ModulePlacementMappingAp1
package org.fog.test.perfeval;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacement;

public class ModulePlacementMappingAp1 extends ModulePlacement{

	private ModuleMapping moduleMapping;
	
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


	public ModulePlacementMappingAp1(List<FogDevice> fogDevices, List<Sensor> sensors, List<Actuator> actuators, 
			Application application, ModuleMapping moduleMapping){
		this.setFogDevices(fogDevices);
		this.setApplication(application);
		this.setModuleMapping(moduleMapping);
		this.setModuleToDeviceMap(new HashMap<String, List<Integer>>());
		this.setDeviceToModuleMap(new HashMap<Integer, List<AppModule>>());
		this.setModuleInstanceCountMap(new HashMap<Integer, Map<String, Integer>>());
		for(FogDevice device : getFogDevices())
			getModuleInstanceCountMap().put(device.getId(), new HashMap<String, Integer>());
		
		switch ( FogSim.SheduleMethod) {
		case 0:
			moduleMapping.addModuleToDevice("user_interface", "cloud"); // fixing all instances of the user_interface module to the Cloud
			for(FogDevice device : fogDevices){
				if(device.getName().startsWith("m-D")){
					moduleMapping.addModuleToDevice("motion_detector", device.getName());  
				}
				if(device.getName().startsWith("d")){
					moduleMapping.addModuleToDevice("object_detector", device.getName()); // placing all instances of Object Detector module in the FogDevices
					moduleMapping.addModuleToDevice("object_tracker", device.getName()); // placing all instances of Object Tracker module in the FogDevices
				}
			}
			break;
		case 1:
			moduleMapping.addModuleToDevice("user_interface", "cloud"); // fixing all instances of the user_interface module to the Cloud
			moduleMapping.addModuleToDevice("motion_detector","cloud" );  
			moduleMapping.addModuleToDevice("object_detector","cloud" ); // placing all instances of Object Detector module in the FogDevices
			moduleMapping.addModuleToDevice("object_tracker","cloud" ); // placing all instances of Object Tracker module in the FogDevices
//			moduleMapping.addModuleToDevice("object_detector", "cloud"); // placing all instances of Object Tracker module in the FogDevices
//			moduleMapping.addModuleToDevice("object_tracker", "cloud"); 
//			moduleMapping.addModuleToDevice("user_interface", "cloud"); // fixing all instances of the user_interface module to the Cloud
//
//			for(FogDevice device : fogDevices){
//				if(device.getName().startsWith("m-D")){
//					moduleMapping.addModuleToDevice("motion_detector", device.getName());  
//				}
////				if(device.getName().startsWith("d")){
////					moduleMapping.addModuleToDevice("object_tracker", device.getName()); // placing all instances of Object Tracker module in the FogDevices
////					moduleMapping.addModuleToDevice("object_detector",device.getName()); // placing all instances of Object Detector module in the FogDevices
////				}
//			}
			break;
		case 2:
			moduleMapping.addModuleToDevice("user_interface", "cloud"); // fixing all instances of the user_interface module to the Cloud
			moduleMapping.addModuleToDevice("motion_detector","cloud" );  
			moduleMapping.addModuleToDevice("object_detector","cloud" ); // placing all instances of Object Detector module in the FogDevices
			moduleMapping.addModuleToDevice("object_tracker","cloud" ); // placing all instances of Object Tracker module in the FogDevices
			break;
		}
		mapModules();
	}
	
	public ModuleMapping getModuleMapping() {
		return moduleMapping;
	}
	public void setModuleMapping(ModuleMapping moduleMapping) {
		this.moduleMapping = moduleMapping;
	}

	
}
