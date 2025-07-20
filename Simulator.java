import java.util.*;

public class Simulator {
    int debug = 0;
    int totalArrivals = 0;
    double currentTime = 0;
    double dayEnd = 24 * 60; // 24 hours in minutes
    double numDays = 1; // default to 1 day for simulation
    double simulationEndTime;
    int warmUpDays = 30;   // number of days to ignore as warm-up
    double warmUpEndTime; // cutoff time in minutes (warmUpDays * 24 * 60)
    double lwbsReevaluationPeriod = 30;


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

    enum StationName {
        SORT,
        REGISTRATION,
        TRIAGE,
        FAST_TRACK,
        ERU,
        RED,
        GREEN,
        BLUE,
        ED,
        ZONE,
        NONE // Default station name
    }
   

    public Simulator() {
        edDisposedPatients = new ArrayList<>();
        steadyStateDisposedPatients = new ArrayList<>();
        eventList = new PriorityQueue<>();

        warmUpEndTime = warmUpDays * 24 * 60;

        eruZone = new Zone(StationName.ERU, this);
        fastTrackZone = new Zone(StationName.FAST_TRACK, this);
        redZone = new Zone(StationName.RED, this);
        greenZone = new Zone(StationName.GREEN, this);

        triage = new Triage(this);
        registration = new Registration(this);
        sortNurse = new SortNurse(this);
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
        simulationEndTime = numDays * dayEnd;
        while (currentTime < simulationEndTime) {
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
                    case registerDeparture:
                    case triageDeparture:
                    case zoneDeparture:
                        getStationByName(currentEvent.patient.currentStationName).departServiceStation(currentEvent);
                        break;
                    case decideToLWBS:
                        currentEvent.patient.processLWBSDecision(this);
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
        if (!patient.isCountedDisposed) {
            edDisposedPatients.add(patient);
            patient.isCountedDisposed = true;
            if (patient.zoneDT >= warmUpEndTime) {
                steadyStateDisposedPatients.add(patient);
            }
        }
    }

    public ServiceStation getStationByName(Simulator.StationName stationName) {
        return switch (stationName) {
            case SORT -> sortNurse;
            case REGISTRATION -> registration;
            case TRIAGE -> triage;
            case FAST_TRACK -> fastTrackZone;
            case GREEN -> greenZone;
            case RED -> redZone;
            case ERU -> eruZone;
            default -> throw new IllegalArgumentException("Unknown station name: " + stationName);
        };
    }

    public void printQuickStats() {
        System.out.println("Days simulated: " + numDays);

        int totalDisposed = edDisposedPatients.size();
        System.out.println("Total arrivals: " + totalArrivals);
        System.out.println("Avg arrivals per day: " + (totalArrivals / (double) numDays));
        System.out.println("Total patients disposed by ED: " + totalDisposed);
        System.out.println("Avg patients disposed per day: " + (totalDisposed / (double) numDays));
        System.out.println("% Disposed: " + String.format("%.2f", (totalDisposed / (double) totalArrivals) * 100) + "%");
        System.out.println("Avg ED Mean Door-to-Provider time: " +
                Utils.formatMinsToHours(Statistics.calculateMean(edDisposedPatients, Simulator.StationName.ED, Statistics.Property.DOOR_TO_PROVIDER_TIME)));
        System.out.println("Avg ED Mean LOS time: " +
                Utils.formatMinsToHours(Statistics.calculateMean(edDisposedPatients, Simulator.StationName.ED, Statistics.Property.RESPONSE_TIME)));

        int totalDeaths = Statistics.countDeaths(edDisposedPatients);
        System.out.println("Total deaths: " + totalDeaths);
        System.out.println("Avg deaths per day: " + (totalDeaths / (double) numDays));
        double lwbs = getTotalLWBSPatients();
        System.out.println("Total LWBS: " + lwbs);
        System.out.println("Avg LWBS per day: " + (lwbs / (double) numDays));
        System.out.println("% LWBS: " + String.format("%.2f", (lwbs / (double) totalArrivals) * 100) + "%");

        double totalUnprocessedPatients = (
                sortNurse.queue.size() +
                        registration.queue.size() +
                        triage.queue.size() + getTotalPatientsInWaitingAreas()
        );
        System.out.println("Total unprocessed patients in ED: " + totalUnprocessedPatients);
        System.out.println("Last event time: " + currentTime);
        System.out.println("Events unprocessed: " + eventList.size());
    }

    public void printQuickStats(Simulator.StationName stationName) {
        if(stationName == Simulator.StationName.ED) {
            System.out.println("\n====== OVERALL ED SUMMARY ======");
            this.printQuickStats();
            return;
        }
        ServiceStation station = getStationByName(stationName);
        if (station != null) {
            System.out.println("\n====== " + stationName + " STATION SUMMARY ======");
            station.printQuickStats();
            return;
        }
    }

    public void printQuickStats(Simulator.StationName[] stationNames) {
        for (Simulator.StationName stationName : stationNames) {
            printQuickStats(stationName);
        }
    }

    public int getTotalPatientsInWaitingAreas(){
        return eruZone.queue.size() + redZone.queue.size() + greenZone.queue.size() + fastTrackZone.queue.size() + triage.queue.size() + registration.queue.size() + sortNurse.queue.size();
    }

    public int getTotalLWBSPatients() {
        int totalLWBS = 0;
        for (ServiceStation station : List.of(sortNurse, registration, triage, eruZone, redZone, greenZone, fastTrackZone)) {
            if (station.lwbsPatients != null) {
                totalLWBS += station.lwbsPatients.size();
            }
        }
        return totalLWBS;
    }

    public void runForDays(int numDays) {
        this.numDays = numDays;
        begin();
    }

    public void printDisposedPatientsLWBSProb(int numPatients) {
        for (int i = 0; i < numPatients && i < edDisposedPatients.size(); i++) {
            Patient patient = edDisposedPatients.get(i);
            System.out.println("\n====== Patient " + patient.id + " Debug Info ======");
            patient.printDebugInfo();

        }
    }

    public static void main(String[] args) {
        Simulator sim = new Simulator();
        sim.runForDays(365);
        sim.printQuickStats(new Simulator.StationName[]{Simulator.StationName.ED, Simulator.StationName.SORT,
                Simulator.StationName.REGISTRATION, Simulator.StationName.TRIAGE,
                Simulator.StationName.FAST_TRACK, Simulator.StationName.RED,
                Simulator.StationName.GREEN, Simulator.StationName.ERU});
        // sim.printDisposedPatientsLWBSProb(10);
    }
}
