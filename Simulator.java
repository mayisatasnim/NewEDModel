import java.util.*;

public class Simulator {
    int debug = 0;
    int totalArrivals = 0;
    double currentTime = 0;
    double dayEnds = 24 * 60;


    // event calendar
    PriorityQueue<Event> eventList;
    List<Patient> edDisposedPatients;
    List<Patient> steadyStateDisposedPatients;
    Registration registration;
    SortNurse sortNurse;
    Triage triage;
    Zone fastTrackZone;
    Zone eruZone;
    Zone redZone;
    Zone greenZone;

    enum zoneName {
        FAST_TRACK,
        ERU,
        RED,
        GREEN
    }

    // WARM-UP configuration
    int warmUpDays = 1000;                  // number of days to ignore as warm-up
    double warmUpEndTime;                // cutoff time in minutes (warmUpDays * 24 * 60)

    public Simulator() {
        edDisposedPatients = new ArrayList<>();
        steadyStateDisposedPatients = new ArrayList<>();
        eventList = new PriorityQueue<>();

        warmUpEndTime = warmUpDays * 24 * 60;

        eruZone = new Zone(zoneName.ERU, edDisposedPatients, eventList, this);
        fastTrackZone = new Zone(zoneName.FAST_TRACK, edDisposedPatients, eventList, this);
        redZone = new Zone(zoneName.RED, edDisposedPatients, eventList, this);
        greenZone = new Zone(zoneName.GREEN, edDisposedPatients, eventList, this);

        triage = new Triage(eruZone, redZone, greenZone, fastTrackZone, eventList);
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

    public void begin() {
        while (currentTime < dayEnds) {
            if (!eventList.isEmpty()) {
                Event currentEvent = eventList.poll();
                currentTime = currentEvent.eventTime;

                staff(currentTime);

                switch (currentEvent.type) {
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


    }

    public void scheduleNextEDArrival() {
        double interEDArrivalTime = Utils.getExp(getArrivalRateByTime(currentTime));
        double nextEDArrivalTime = currentTime + interEDArrivalTime;
        Patient newPatient = new Patient(totalArrivals);
        eventList.add(new Event(nextEDArrivalTime, Event.EventType.edArrival, newPatient));
        totalArrivals++;
        if (debug == 1) {
            System.out.println("\n[Simulator]: Next ED-AT: " + nextEDArrivalTime + "\n");
        }
    }

    // dynamic arrival time
    public static double getArrivalRateByTime(double currentTime) {
        int hour = (int) ((currentTime / 60.0) % 24);

        return switch (hour) {
            case 0, 7 -> 5.0 / 60.0;
            case 1 -> 4.5 / 60.0;
            case 3 -> 3.0 / 60.0;
            case 2, 4, 5, 6 -> 4.0 / 60.0;
            case 8, 22 -> 7.0 / 60.0;
            case 9 -> 10.0 / 60.0;
            case 10, 14, 15, 16 -> 13.0 / 60.0;
            case 11, 12, 13 -> 14.0 / 60.0;
            case 17 -> 12.0 / 60.0;
            case 18, 19 -> 11.0 / 60.0;
            case 20 -> 9.0 / 60.0;
            case 21 -> 8.0 / 60.0;
            case 23 -> 6.0 / 60.0;
            default -> 10.0 / 60.0;
        };

    }

    // dynamic staffing
    public void staff(double currentTime) {
        int hour = (int) ((currentTime / 60.0) % 24);

        if (hour >= 0 && hour < 7) {
            redZone.setStaffAvailable(13);
            greenZone.setStaffAvailable(10);
            fastTrackZone.setStaffAvailable(5);
            eruZone.setStaffAvailable(4);
        } else if (hour >= 7 && hour < 15) {
            redZone.setStaffAvailable(13);
            greenZone.setStaffAvailable(10);
            fastTrackZone.setStaffAvailable(5);
            eruZone.setStaffAvailable(4);
        } else {
            redZone.setStaffAvailable(8);
            greenZone.setStaffAvailable(6);
            fastTrackZone.setStaffAvailable(3);
            eruZone.setStaffAvailable(2);
        }
    }

    //used to get patients after warm up stage
    public void addDisposedPatient(Patient patient) {
        edDisposedPatients.add(patient);
        if (patient.zoneDT >= warmUpEndTime) {
            steadyStateDisposedPatients.add(patient);
        }
    }

    public void printQuickStats(int numDays) {
        System.out.println("\n===DAY's SIMULATION SUMMARY (STEADY STATE ONLY, AFTER WARM-UP) ===");
        System.out.println("Days simulated: " + numDays);

        int totalDisposed = edDisposedPatients.size();
        System.out.println("Total patients disposed by ED: " + totalDisposed);
        System.out.println("Avg patients disposed per day: " + (totalDisposed / (double) numDays));

        // If totalArrivals is cumulative over the entire simulation
        System.out.println("Total arrivals: " + totalArrivals);
        System.out.println("Avg arrivals per day: " + (totalArrivals / (double) numDays));

        System.out.println("Avg ED Mean Door-to-Provider time: " +
                Statistics.calculateMean(edDisposedPatients, Statistics.Stage.ED, Statistics.Property.DOOR_TO_PROVIDER_TIME));
        System.out.println("Avg ED Mean LOS time: " +
                Statistics.calculateMean(edDisposedPatients, Statistics.Stage.ED, Statistics.Property.RESPONSE_TIME));

        int totalDeaths = Statistics.countDeaths(edDisposedPatients);
        System.out.println("Total deaths: " + totalDeaths);
        System.out.println("Avg deaths per day: " + (totalDeaths / (double) numDays));

        int totalLWBS = Statistics.countLWBS(edDisposedPatients);
        System.out.println("Total LWBS: " + totalLWBS);
        System.out.println("Avg LWBS per day: " + (totalLWBS / (double) numDays));

        double totalUnprocessedPatients = (
                sortNurse.sortNQueue.size() +
                        registration.regQueue.size() +
                        triage.triageQueue.size() +
                        eruZone.zoneQueue.size() +
                        redZone.zoneQueue.size() +
                        greenZone.zoneQueue.size() +
                        fastTrackZone.zoneQueue.size()
        );
        System.out.println("Total unprocessed patients in ED: " + totalUnprocessedPatients);
        System.out.println("Last event time: " + currentTime);
        System.out.println("Events unprocessed: " + eventList.size());

        System.out.println("\n=== QUICK STAGE OF SIMULATION SUMMARY ===\n");
        eruZone.printQuickStats(numDays);
        redZone.printQuickStats(numDays);
        greenZone.printQuickStats(numDays);
        fastTrackZone.printQuickStats(numDays);
    }



    public static void runContinuousSimulation(int numDays) {
        Simulator sim = new Simulator();
        sim.runForDays(numDays);
    }

    public void runForDays(int numDays) {
        dayEnds = numDays * 24 * 60;
        begin();
        printQuickStats(numDays);     // print stats averaged per day
    }

    public static void main(String[] args) {
        runContinuousSimulation(10000);
    }
}
