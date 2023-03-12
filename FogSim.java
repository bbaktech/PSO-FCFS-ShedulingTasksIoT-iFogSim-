//FogSim.java derived from CloudSim
package org.fog.test.perfeval;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.core.predicates.Predicate;

public class FogSim extends CloudSim {
	
    protected static long iteration_count = 0;  //by manju Manju
    public static  int SheduleMethod = 0;  // 0->FCFS,1->PS,2->WOA
    
    
	protected static void update_futureQueByShaduling()
	{
		int dest, src;
		SimEntity entity;
		int entitys_size = entities.size();
		int [] fogSheduledevises = new int [50] ;
		int dFogCount	 = 0;
		
		//d-R:{i} is the names of fogdevises		
		for (int i = 0; i< entitys_size; i++)
		{   		    
			entity = entities.get(i);
			if (entity.getName().startsWith("d")) {			
				fogSheduledevises[dFogCount] = entity.getId();
				dFogCount ++;
			}
		}		

		Iterator<SimEvent> fit = future.iterator();

		int nexttaskaulter = 0;	
		
		while (fit.hasNext()) {
			SimEvent e = fit.next();
			dest = e.getDestination();			
			if (dest == fogSheduledevises[0]) {				
				  int tag = e.getTag();
				  switch (tag) {			   
					  case 51:
						  e.setDestination(fogSheduledevises[nexttaskaulter]);
						  break; 
				  } 				  
//				  System.out.println("Dist:"+ e.getDestination() +" source:"+e.getSource()+" getTag():"+e.getTag() +" getType():"+e.getType());
				  nexttaskaulter ++; 
				  if (nexttaskaulter == dFogCount) { nexttaskaulter = 0; }
				 			
			}
		}
	}
	
	public static boolean runClockTick() {
		SimEntity ent;
		boolean queue_empty = false ;
		
		int entities_size = entities.size();
		
//		if (iteration_count< 50) 
//			System.out.println( "=================Sarted Slot:" + iteration_count); 
		
		for (int i = 0; i < entities_size; i++ ) {
			ent = entities.get(i);
			if (ent.getState() == SimEntity.RUNNABLE) {
				ent.run();
			}

		}		
		//resourse sheduling WOS
		switch (SheduleMethod) {
		case 0: //FCFS
			update_futureQueByShaduling();
			queue_empty = executeAllTasks();
			break;
		case 1:
			update_futureQueByShaduling();
			queue_empty = executeHighPriorityTasks();
//			queue_empty= executeLowPriorityTasks();
			break;			
		case 2:
			update_futureQueByShaduling();
			System.out.println("WO Algorithem Not Ready");
			System.exit(0);
			break;
		}
		
		// If there are more future events then deal with them
		return queue_empty;
	}
	public static boolean executeHighPriorityTasks (){
		
		boolean queue_empty;
		if (future.size() > 0) {
			
			List<SimEvent> toRemove = new ArrayList<SimEvent>();		
			Iterator<SimEvent> fit = future.iterator();
			
			queue_empty = false;		
			SimEvent first = fit.next();
			SimEntity ent = entities.get( first.getSource());			
			if (ent.getName().startsWith("m-V")) {
				processEvent(first);
				future.remove(first);
			}			
			
			fit = future.iterator();	
			// Check if next events are at same time...
			boolean trymore = fit.hasNext();
			while (trymore) {
				SimEvent next = fit.next();
				ent = entities.get( next.getSource());				
				if (next.eventTime() == first.eventTime()) {
					if (ent.getName().startsWith("m-V")) {
						processEvent(next);
						toRemove.add(next);
					} 
					trymore = fit.hasNext();
				} else {
					trymore = false;
				}
			}
			future.removeAll(toRemove);			
			toRemove.clear();

			Iterator<SimEvent> fit2 = future.iterator();
			trymore = fit2.hasNext();
			while (trymore) {
				SimEvent next = fit2.next();
				if (next.eventTime() == first.eventTime()) {					 
					processEvent(next);
					toRemove.add(next);
					trymore = fit.hasNext();
				} else {
					trymore = false;
				}
			}
			future.removeAll(toRemove);

		} else {
			queue_empty = true;
			running = false;
		}
		// If there are more future events then deal with them
		return queue_empty;
	}
	

	public static boolean executeAllTasks (){
		
		boolean queue_empty;
		if (future.size() > 0) {
			List<SimEvent> toRemove = new ArrayList<SimEvent>();
			Iterator<SimEvent> fit = future.iterator();
			queue_empty = false;
			
			SimEvent first = fit.next();
			processEvent(first);
			future.remove(first);
			fit = future.iterator();
	
			// Check if next events are at same time...
			boolean trymore = fit.hasNext();
			while (trymore) {
				SimEvent next = fit.next();
				if (next.eventTime() == first.eventTime()) {					 
					processEvent(next);
					toRemove.add(next);
					trymore = fit.hasNext();
				} else {
					trymore = false;
				}
			}
			future.removeAll(toRemove);
		} else {
			queue_empty = true;
			running = false;
		}
		// If there are more future events then deal with them
		return queue_empty;
	}
	
	protected static void processEvent(SimEvent e) {
		int dest, src;
		SimEntity dest_ent;
		// Update the system's clock
		
		if (e.eventTime() < clock) {
			throw new IllegalArgumentException("Past event detected.");
		}
		clock = e.eventTime();

		// Ok now process it
		switch (e.getType()) {
			case SimEvent.ENULL:
				throw new IllegalArgumentException("Event has a null type.");

			case SimEvent.CREATE:
				SimEntity newe = (SimEntity) e.getData();
				addEntityDynamically(newe);
				break;
				
			case SimEvent.SEND:
				// Check for matching wait
				dest = e.getDestination();
				if (dest < 0) {
					throw new IllegalArgumentException("Attempt to send to a null entity detected.");
				} else {
					int tag = e.getTag();
					dest_ent = entities.get(dest);
					if (dest_ent.getState() == SimEntity.WAITING) {
						Integer destObj = Integer.valueOf(dest);
						Predicate p = waitPredicates.get(destObj);
						if ((p == null) || (tag == 9999) || (p.match(e))) {
							dest_ent.setEventBuffer((SimEvent) e.clone());
							dest_ent.setState(SimEntity.RUNNABLE);
							waitPredicates.remove(destObj);
						} else {
							deferred.addEvent(e);
						}
					} else {
						deferred.addEvent(e);
					}
				}
				break;

			case SimEvent.HOLD_DONE:
				src = e.getSource();
				if (src < 0) {
					throw new IllegalArgumentException("Null entity holding.");
				} else {
					entities.get(src).setState(SimEntity.RUNNABLE);
				}
				break;

			default:
				break;
		}
	}

	public static double startSimulation() throws NullPointerException {
	
		try {
			System.out.println("===FogSim started====");
			double clock = run();

			// reset all static variables
			cisId = -1;
			shutdownId = -1;
			cis = null;
			calendar = null;
			traceFlag = false;

			return clock;
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			throw new NullPointerException("CloudSim.startCloudSimulation() :"
					+ " Error - you haven't initialized CloudSim.");
		}
	}
	
	public static double run() {
		if (!running) {
			runStart();
		}
		while (true) {
			
			if (runClockTick() || abruptTerminate) {
				break;
			}
			
			// this block allows termination of simulation at a specific time
			if (terminateAt > 0.0 && clock >= terminateAt) {
				terminateSimulation();
				clock = terminateAt;
				break;
			}

			if (pauseAt != -1
					&& ((future.size() > 0 && clock <= pauseAt && pauseAt <= future.iterator().next()
							.eventTime()) || future.size() == 0 && pauseAt <= clock)) {
				pauseSimulation();
				clock = pauseAt;
			}

			while (paused) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			iteration_count++;
		}

		double clock = clock();

		finishSimulation();
		runStop();

		return clock;
	}
}
