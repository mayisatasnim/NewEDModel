import java.util.*;

public class Simulator {
    int debug = 0;
    int totalArrivals = 0;
    double arrivalRate = 12 / 60.0;
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

        eruZone = new Zone(zoneName.ERU, edDisposedPatients, eventList);
        fastTrackZone = new Zone(zoneName.FAST_TRACK, edDisposedPatients, eventList);
        redZone = new Zone(zoneName.RED, edDisposedPatients, eventList);
        greenZone = new Zone(zoneName.GREEN, edDisposedPatients, eventList);

        triage = new Triage(eruZone,redZone,greenZone, fastTrackZone, eventList);
        registration = new Registration(triage, eventList);
        sortNurse = new SortNurse(registration, eruZone, eventList);
        configureServiceTimes();
        scheduleNextEDArrival();
    }

    public void configureServiceTimes() {
        sortNurse.setServiceTime(4, 1);
        registration.setServiceTime(5, 1);
        triage.setServiceTime(13, 2);
        eruZone.setServiceTime(134, 30);
        redZone.setServiceTime(192, 30);
        greenZone.setServiceTime(200, 30);
        fastTrackZone.setServiceTime(129, 30);
    }

    public void begin(){

        while (currentTime < dayEnds) {
            if (!eventList.isEmpty()){
                Event currentEvent = eventList.poll();
                currentTime = currentEvent.eventTime;

                staff(currentTime);

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
        printQuickStats();
    }

    public void scheduleNextEDArrival(){
       // double interEDArrivalTime = Utils.getExp(arrivalRate);
        double interEDArrivalTime = Utils.getExp(getArrivalRateByTime(currentTime));

        double nextEDArrivalTime = currentTime + interEDArrivalTime;
        Patient newPatient = new Patient(totalArrivals);
        eventList.add(new Event(nextEDArrivalTime,Event.EventType.edArrival, newPatient));
        totalArrivals++;
        if (debug == 1) {
            System.out.println("\n[Simulator]: Next ED-AT: " + nextEDArrivalTime+"\n");
        }
    }

    //dynamic arrival time
    public double getArrivalRateByTime(double currentTime) {
        int hour = (int) ((currentTime / 60.0) % 24);

        switch(hour){
            case 0,7: return 5.0 / 60.0;
            case 1: return 4.5 / 60.0;
            case 2: return 4.0 / 60.0;
            case 3: return 3.0 / 60.0;
            case 4, 5, 6: return 4.0 / 60.0;
            case 8, 22: return 7.0 / 60.0;
            case 9: return 10.0 / 60.0;
            case 10, 14, 15, 16: return 13.0 / 60.0;
            case 11, 12, 13: return 14.0 / 60.0;
            case 17: return 12.0 / 60.0;
            case 18, 19: return 11.0 / 60.0;
            case 20: return 9.0 / 60.0;
            case 21: return 8.0 / 60.0;
            case 23: return 6.0 / 60.0;
        }

        return 10.0 / 60.0;
    }

    //dynamic staffing
    public void staff(double currentTime){
        int hour = (int) ((currentTime / 60.0) % 24);

        //overnight
        if (hour >= 0 && hour < 7) {
            redZone.setStaffAvailable(13);
            greenZone.setStaffAvailable(10);
            fastTrackZone.setStaffAvailable(5);
            eruZone.setStaffAvailable(4);

            //morning + afternoon shift
        } else if (hour >= 7 && hour < 15){
            redZone.setStaffAvailable(13);
            greenZone.setStaffAvailable(10);
            fastTrackZone.setStaffAvailable(5);
            eruZone.setStaffAvailable(4);
        }else {
            //evening shift
            redZone.setStaffAvailable(8);
            greenZone.setStaffAvailable(6);
            fastTrackZone.setStaffAvailable(3);
            eruZone.setStaffAvailable(2);
        }
    }

    public static void main(String[]args){
        Simulator sim = new Simulator();
        sim.begin();
    }

    public void printQuickStats() {
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
        System.out.println("ED Mean Door-to-Provider time: " + Statistics.calculateMean(edDisposedPatients, Statistics.Stage.ED, Statistics.Property.DOOR_TO_PROVIDER_TIME));
        System.out.println("ED Mean LOS time: " + Statistics.calculateMean(edDisposedPatients, Statistics.Stage.ED, Statistics.Property.RESPONSE_TIME));
        System.out.println("Last event time: " + currentTime);
        System.out.println("Events unprocessed: "+ eventList.size());
        if (debug ==1 ) {
            System.out.println("Remaining events:");
            for (Event e : eventList) {
                System.out.println("[" + e.type + "] " + e.patient.id + " @T: " + e.eventTime);
            }
        }
        //total death count
        System.out.println("Total deaths: " + Statistics.countDeaths(edDisposedPatients));
        //total lwbs
        System.out.println("Total LWBS: " + Statistics.countLWBS(edDisposedPatients));


        System.out.println("\n=== QUICK STAGE OF SIMULATION SUMMARY ===\n");

        sortNurse.printQuickStats();
        registration.printQuickStats();
        triage.printQuickStats();

        eruZone.printQuickStats();
        redZone.printQuickStats();
        greenZone.printQuickStats();
        fastTrackZone.printQuickStats();
    }
}