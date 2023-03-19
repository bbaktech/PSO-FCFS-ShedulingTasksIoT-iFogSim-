//ModulePlacementMappingAp0

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

public class ModulePlacementMappingAp0 extends ModulePlacement{

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

	public ModulePlacementMappingAp0(List<FogDevice> fogDevices, List<Sensor> sensors, List<Actuator> actuators, 
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
			moduleMapping.addModuleToDevice("coordinator", "cloud"); // fixing all instances of the coordinator module to the Cloud
			for(FogDevice device : fogDevices){
				if(device.getName().startsWith("m-V")){
					moduleMapping.addModuleToDevice("client", device.getName());  // fixing all instances of the Client module to the Smartphones
				}
				if(device.getName().startsWith("d")){
					moduleMapping.addModuleToDevice("concentration_calculator", device.getName()); // fixing all instances of the Concentration Calculator module to the FogDevices
				}
			}

			break;
		case 1:
			moduleMapping.addModuleToDevice("coordinator", "cloud"); // fixing all instances of the coordinator module to the Cloud
			for(FogDevice device : fogDevices){
				if(device.getName().startsWith("m-V")){
					moduleMapping.addModuleToDevice("client", device.getName());  // fixing all instances of the Client module to the Smartphones
				}
				if(device.getName().startsWith("d")){
					moduleMapping.addModuleToDevice("concentration_calculator", device.getName()); // fixing all instances of the Concentration Calculator module to the FogDevices
				}
			}

			break;
		case 2:
			moduleMapping.addModuleToDevice("coordinator", "cloud"); // fixing all instances of the coordinator module to the Cloud
			moduleMapping.addModuleToDevice("client", "cloud");  // fixing all instances of the Client module to the Smartphones
			moduleMapping.addModuleToDevice("concentration_calculator", "cloud"); // fixing all instances of the Concentration Calculator module to the FogDevices
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
