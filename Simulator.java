import java.util.*;

public class Simulator {
    int debug = 1;
    double arrivalRate = 10 / 60.0;
    double currentTime = 0;
    double dayEnds = 24 * 60;
    double nextArrivalTime = 0;
    int totalArrivals = 0;
    SortNurse sortNurse;
    Registration registration;
    //event calendar
    PriorityQueue<Event> eventList;



    public Simulator(){
        eventList = new PriorityQueue<Event>();
        registration = new Registration();
        sortNurse = new SortNurse(50,registration,eventList);

        //schedule first arrival
        scheduleNextEDArrival();
    }

    public void begin(){

        while (currentTime < dayEnds) {
            if (!eventList.isEmpty()){
                Event currentEvent = eventList.poll();
                currentTime = currentEvent.eventTime;
                switch (currentEvent.type){
                    case edArrival:
                        sortNurse.addPatient(currentEvent);
                        scheduleNextEDArrival();
                        break;
                    case sortDeparture:
                        sortNurse.departSortingNurse(currentEvent);
                        break;
                    default:
                        System.out.println("unknown event");
                }
            }
        }
        
        // some statistics
        if(debug == 1) {
            System.out.println("\n===Day's SIMULATION SUMMARY ===");
            System.out.println("Total arrivals: " + totalArrivals);
            System.out.println("Last event time: " + currentTime);
            System.out.println("Events unprocessed: "+ eventList.size());
            sortNurse.printQuickStats();
        }
    }

    public void scheduleNextEDArrival(){
        double interEDArrivalTime = Utils.getExp(10/60.0);
        double nextEDArrivalTime = currentTime + interEDArrivalTime;
        Patient newPatient = new Patient(totalArrivals);
        eventList.add(new Event(nextEDArrivalTime,Event.EventType.edArrival, newPatient));
        totalArrivals++;
        if (debug == 1) {
            System.out.println("\n[Simulator]: Next ED-AT: " + nextEDArrivalTime+"\n");
        }
    }
    public static void main(String[]args){
        Simulator sim = new Simulator();
        sim.begin();
    }
}
