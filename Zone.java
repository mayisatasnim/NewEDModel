import java.util.*;

public class Zone {
    private int debug = 1;

    private Patient currentPatient;
    private boolean isOccupied;
    private final double meanZoneTime = 4.0;
    private final double zoneStdDev = 1.0;
    public PriorityQueue<Patient> zoneQueue;
    public List<Patient> zoneDepartedPatients;
    public List<Patient> edDisposedPatients;
    PriorityQueue<Event> eventList;
    Simulator.zoneName zoneName;

    public Zone(Simulator.zoneName zoneName, int queueCapacity, List<Patient> edDisposedPatients, PriorityQueue<Event> eventList) {
        this.zoneQueue = new PriorityQueue<>(Comparator.comparingDouble(p -> p.ESILevel));
        this.zoneDepartedPatients = new ArrayList<Patient>();
        this.edDisposedPatients = edDisposedPatients;
        this.currentPatient = null;
        this.zoneName = zoneName;
        this.isOccupied = false;
        this.eventList = eventList;
    }

    public void addPatient(Event currentEvent) {
        zoneQueue.add(currentEvent.patient);
        currentEvent.patient.zoneAT = currentEvent.eventTime;
        currentEvent.patient.zoneDT = Double.POSITIVE_INFINITY;
        currentEvent.patient.zoneName = zoneName;
        System.out.println("[Zone]: Added " + currentEvent.patient.id + " to zoneQueue @T: " + currentEvent.eventTime);
        if(!isOccupied){
            Patient patientDepartingNext = zoneQueue.poll();
            patientDepartingNext.zonePT = currentEvent.eventTime;
            currentPatient = patientDepartingNext;
            scheduleNextDeparture(currentEvent.eventTime, patientDepartingNext);
        }
    }

    public void scheduleNextDeparture(double currentTime, Patient patient){
        double serviceTime = Utils.getNormal(meanZoneTime, zoneStdDev);
        double nextDepartureTime = currentTime + serviceTime;        
        eventList.add(new Event(nextDepartureTime, Event.EventType.zoneDeparture, patient));
        isOccupied = true;
        if(debug == 1){ System.out.println("[Zone]: Next zoneDT: " + nextDepartureTime);}
    }

    public void departZone(Event currentEvent) {
        if(currentPatient != currentEvent.patient) {
            throw new IllegalStateException("[Zone-ERROR]: Got " + currentEvent.patient.id + " AT@ " + currentEvent.patient.zoneAT + " [!=] \n Expected " + currentPatient.id + " AT@ " + currentPatient.zoneAT);
        }
        if(debug == 1) {System.out.println(currentEvent.patient.id + " DP_zone: " + currentEvent.eventTime);}
        // process departure
        edDisposedPatients.add(currentEvent.patient);
        currentEvent.patient.zoneDT = currentEvent.eventTime;
        zoneDepartedPatients.add(currentEvent.patient);
        isOccupied = false;
        currentPatient = null;
    }

    public void printQuickStats() {
        System.out.println("\n[Zone-" + zoneName + "]: Quick Stats");
        System.out.println("Total patients processed: " + zoneDepartedPatients.size());
        System.out.println("Current zoneQueue size[if end of day indicates unprocessed patients]: " + zoneQueue.size());
        System.out.println("Mean zone waiting time: " + Statistics.calculateAverage(zoneDepartedPatients, Statistics.Stage.ZONE, Statistics.Property.WAITING_TIME));
        System.out.println("Mean zone service time: " + Statistics.calculateAverage(zoneDepartedPatients, Statistics.Stage.ZONE, Statistics.Property.SERVICE_TIME));
        System.out.println("Mean zone LOS: " + Statistics.calculateAverage(zoneDepartedPatients, Statistics.Stage.ZONE, Statistics.Property.LOS));
    }
}
