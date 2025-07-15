import java.util.*;

public class Simulator {
    int debug = 1;
    int totalArrivals = 0;
    double arrivalRate = 10 / 60.0;
    double currentTime = 0;
    double dayEnds = 24 * 60;
    double nextArrivalTime = 0;
    //event calendar
    PriorityQueue<Event> eventList;
    List<Patient> edDisposedPatients;
    Registration registration;
    SortNurse sortNurse;
    Triage triage;
    Zone fastTrackZone;
    Zone eruZone;
    Zone redZone;
    Zone greenZone;
    enum zoneName{
        FAST_TRACK,
        ERU,
        RED,
        GREEN
    }

    public Simulator(){
        edDisposedPatients = new ArrayList<Patient>();
        eventList = new PriorityQueue<Event>();
        eruZone = new Zone(zoneName.ERU,40, edDisposedPatients,eventList);
        greenZone = new Zone(zoneName.GREEN,40, edDisposedPatients,eventList);
        redZone = new Zone(zoneName.RED,40, edDisposedPatients,eventList);
        fastTrackZone = new Zone(zoneName.FAST_TRACK,40, edDisposedPatients,eventList);
        triage = new Triage(100, eruZone,redZone,greenZone, fastTrackZone, eventList);
        registration = new Registration(100, triage, eventList);
        sortNurse = new SortNurse(100,registration, eruZone, eventList);
        configureServiceTimes();
        scheduleNextEDArrival();
    }

    public void configureServiceTimes() {
        sortNurse.setServiceTime(0, 0);
        registration.setServiceTime(0, 0);
        triage.setServiceTime(10.0, 5.0);
        eruZone.setServiceTime(4.0, 1.0);
        redZone.setServiceTime(4.0, 1.0);
        greenZone.setServiceTime(4.0, 1.0);
        fastTrackZone.setServiceTime(4.0, 1.0);
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
                    case registerDeparture:
                        registration.departRegistration(currentEvent);
                        break;
                    case triageDeparture:
                        triage.departTriage(currentEvent);
                        break;
                    case zoneDeparture:
                        switch (currentEvent.patient.zoneName) {
                            case ERU:
                                eruZone.departZone(currentEvent);
                                break;
                            case RED:
                                redZone.departZone(currentEvent);
                                break;
                            case GREEN:
                                greenZone.departZone(currentEvent);
                                break;
                            case FAST_TRACK:
                                fastTrackZone.departZone(currentEvent);
                                break;
                            default:
                                System.out.println("[Simulator-ERROR]: Unknown zone");
                                break;
                        }
                        break;
                    default:
                        System.out.println("[Simulator-ERROR]: unknown event");
                }
            }
        }
        
        // some statistics
        if(debug == 1) {
            System.out.println("\n===DAY's SIMULATION SUMMARY ===");
            System.out.println("ED Total arrivals: " + totalArrivals);
            System.out.println("Total patients disposed by ED: " + edDisposedPatients.size());
            double totalUnprocessedPatients = (
                sortNurse.sortNQueue.size() + 
                registration.regQueue.size() +
                triage.triageQueue.size() + 
                eruZone.zoneQueue.size() +
                redZone.zoneQueue.size() +
                greenZone.zoneQueue.size() +
                fastTrackZone.zoneQueue.size());
            System.out.println("Total unprocessed patients in ED: " + totalUnprocessedPatients);
            System.out.println("ED Mean Door-to-Provider time: " + Statistics.calculateAverage(edDisposedPatients, Statistics.Stage.ED, Statistics.Property.DOOR_TO_PROVIDER_TIME));
            System.out.println("ED Mean LOS time: " + Statistics.calculateAverage(edDisposedPatients, Statistics.Stage.ED, Statistics.Property.LOS));
            System.out.println("Last event time: " + currentTime);
            System.out.println("Events unprocessed: "+ eventList.size());
            System.out.println("\n=== QUICK STAGE OF SIMULATION SUMMARY ===\n");
            // sortNurse.printQuickStats();
            // registration.printQuickStats();
            triage.printQuickStats();
            eruZone.printQuickStats();
            // redZone.printQuickStats();
            // greenZone.printQuickStats();
            // fastTrackZone.printQuickStats();
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
