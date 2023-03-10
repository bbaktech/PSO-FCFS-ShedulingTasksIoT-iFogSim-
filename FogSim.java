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
    
	protected static void update_futureQueByShaduling()
	{
//		if (iteration_count <2) System.out.println("update_futureQue-FogSim");
		int dest, src;
		SimEntity entity;
		int entitys_size = entities.size();
		class fogSheduled {
			public int id;
			public int updaeCnt;
			public fogSheduled() {updaeCnt = 0; }
		};
		
		fogSheduled [] fogSheduledevises = new fogSheduled[50] ;
		int dFogCount	 = 0;
		
		//d-R:{i} is the names of fogdevises		
		for (int i = 0; i< entitys_size; i++)
		{   		    
			entity = entities.get(i);
			if (entity.getName().startsWith("d")) {
				fogSheduledevises[dFogCount] = new fogSheduled();
				fogSheduledevises[dFogCount].id = entity.getId();
				fogSheduledevises[dFogCount].updaeCnt = 0;
				dFogCount ++;
			}
			if (iteration_count< 1) 
				System.out.println(entity.getId() + " :Name: " + entity.getName());
		}		

		Iterator<SimEvent> fit = future.iterator();

		int nexttaskaulter = 0;	
		int duplicate_cont = 0;		
		SimEvent e1 = null;
		
		while (fit.hasNext()) {
			SimEvent e = fit.next();
			dest = e.getDestination();
			
			if (dest == fogSheduledevises[0].id) {
				
				  int tag = e.getTag();
				  switch (tag) {			  
//					  case 51: 
					  case 68:
						  e1 = (SimEvent)
						  e.clone(); 
						  break; 
					  case 62:
						  e.setDestination(fogSheduledevises[nexttaskaulter].id);
						  fogSheduledevises[nexttaskaulter].updaeCnt ++; 
						  break; 
					  case 56:
						  e.setDestination(fogSheduledevises[nexttaskaulter].id);
						  fogSheduledevises[nexttaskaulter].updaeCnt ++; 
						  break; 
					  case 52:
						  e.setDestination(fogSheduledevises[nexttaskaulter].id);
						  fogSheduledevises[nexttaskaulter].updaeCnt ++; 
						  break; 
					  case 74: 
						  e1 =  (SimEvent) e.clone(); e.setDestination(fogSheduledevises[nexttaskaulter].id);
						  fogSheduledevises[nexttaskaulter].updaeCnt ++; 
						  break;
				  } 
				  
				  if (iteration_count ==500) System.out.println("Old Dist:"+ dest +" New Dest:"+e.getDestination()+" getTag():"+e.getTag() +" getType():"+e.getType());
				  
				  if (3 == fogSheduledevises[nexttaskaulter].updaeCnt) {
					  fogSheduledevises[nexttaskaulter].updaeCnt = 0; nexttaskaulter ++; 
					  if  (duplicate_cont <dFogCount) duplicate_cont++; 
					 }
				 			
			}
			
			if (nexttaskaulter == dFogCount) { nexttaskaulter = 0; }
		}
		
		if ( e1 != null) {
			for (int indx = 1 ;indx <  dFogCount ;indx++) {
				SimEvent e2 = (SimEvent) e1.clone();
				//SimEvent e1 = new SimEvent(SimEvent.SEND, clock + delay, src, dest, tag, data);
				e2.setDestination(fogSheduledevises[indx].id);
				future.addEvent(e2);
			}
		}

	}

	public static boolean runClockTick() {
		SimEntity ent;
		boolean queue_empty;
		
		int entities_size = entities.size();
		
		if (iteration_count< 20) System.out.println( "=================Sarted Slot:" + iteration_count); 
		
		for (int i = 0; i < entities_size; i++ ) {
			ent = entities.get(i);
			if (ent.getState() == SimEntity.RUNNABLE) {
				if (iteration_count ==0) System.out.println( "Run Device:"+ ent.getName()); 
				ent.run();
			}

		}
		
		//resourse sheduling WOS
		update_futureQueByShaduling();	
		
		if (iteration_count < 20) 
			System.out.println( "Sheduling tasks for slot ("+ iteration_count + ") Completed...NoTasks:"+future.size());

		// If there are more future events then deal with them
		int excont = 1;
		
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
//					System.out.println(next); 
					excont++;
					processEvent(next);
					toRemove.add(next);
					trymore = fit.hasNext();
				} else {
					trymore = false;
				}
			}
			future.removeAll(toRemove);
			if (iteration_count <20) System.out.println("Ex Cont:"+excont);

		} else {
			queue_empty = true;
			running = false;
		}

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
			System.out.println("FogSim started");
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
		System.out.println("Fog Sim run() started");
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
