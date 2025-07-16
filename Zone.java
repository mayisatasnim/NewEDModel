import java.util.*;

public class Zone extends ServiceStation {
    public PriorityQueue<Patient> zoneQueue;
    public List<Patient> zoneDepartedPatients;
    private List<Patient> edDisposedPatients;
    private Simulator.zoneName zoneName;

    public Zone(Simulator.zoneName zoneName, int numBeds, List<Patient> edDisposedPatients, PriorityQueue<Event> eventList) {
        super("Zone-" + zoneName, 4.0, 1.0, getZoneCapacity(zoneName), eventList);
        this.zoneName = zoneName;
        this.zoneQueue = this.queue;
        this.zoneDepartedPatients = this.departedPatients;
        this.edDisposedPatients = edDisposedPatients;
    }

    private static int getZoneCapacity(Simulator.zoneName zoneName) {
        switch (zoneName) {
            case ERU: return 14;
            case FAST_TRACK: return 43;
            case RED: return 29;
            case GREEN: return 32;
            default: return 1;
        }
    }


    @Override
    protected void setPatientArrivalTime(Patient patient, double time) {
        patient.zoneAT = time;
    }

    @Override
    protected void setPatientDepartureTime(Patient patient, double time) {
        patient.zoneDT = time;
    }

    @Override
    protected void setPatientProcessingTime(Patient patient, double time) {
        patient.zonePT = time;
    }

    @Override
    protected Event.EventType getDepartureEventType() {
        return Event.EventType.zoneDeparture;
    }

    @Override
    protected void processPatientDeparture(Event currentEvent) {
        edDisposedPatients.add(currentEvent.patient);
    }

    @Override
    protected Statistics.Stage getStatisticsStage() {
        return Statistics.Stage.ZONE;
    }

    @Override
    public void addPatient(Event currentEvent) {
        currentEvent.patient.zoneName = zoneName;
        super.addPatient(currentEvent);
    }

    public void departZone(Event currentEvent) {
        departServiceStation(currentEvent);
    }

    @Override
    public void printQuickStats() {
        System.out.println("\n[Zone-" + zoneName + "]: Quick Stats");
        System.out.println("Total arrivals: " + totalArrivals);
        System.out.println("Total processed: " + departedPatients.size());
        System.out.println("Current Queue size[waiting]: " + queue.size());
        System.out.println("Mean zone waiting time: " + Statistics.calculateMean(departedPatients, Statistics.Stage.ZONE, Statistics.Property.WAITING_TIME));
        System.out.println("Mean zone service time: " + Statistics.calculateMean(departedPatients, Statistics.Stage.ZONE, Statistics.Property.PROCESSING_TIME));
        System.out.println("Mean zone LOS: " + Statistics.calculateMean(departedPatients, Statistics.Stage.ZONE, Statistics.Property.RESPONSE_TIME));
    }
}
