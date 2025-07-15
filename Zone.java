import java.util.*;

public class Zone extends ServiceStation {
    public PriorityQueue<Patient> zoneQueue;
    public List<Patient> zoneDepartedPatients;
    private List<Patient> edDisposedPatients;
    private Simulator.zoneName zoneName;

    public Zone(Simulator.zoneName zoneName, int queueCapacity, List<Patient> edDisposedPatients, PriorityQueue<Event> eventList) {
        super("Zone", 4.0, 1.0, eventList);
        this.zoneName = zoneName;
        this.zoneQueue = this.queue;
        this.zoneDepartedPatients = this.departedPatients;
        this.edDisposedPatients = edDisposedPatients;
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
        departPatient(currentEvent);
    }

    @Override
    public void printQuickStats() {
        System.out.println("\n[Zone-" + zoneName + "]: Quick Stats");
        System.out.println("Total patients processed: " + departedPatients.size());
        System.out.println("Current zoneQueue size: " + queue.size());
        System.out.println("Mean zone waiting time: " + Statistics.calculateAverage(departedPatients, Statistics.Stage.ZONE, Statistics.Property.WAITING_TIME));
        System.out.println("Mean zone service time: " + Statistics.calculateAverage(departedPatients, Statistics.Stage.ZONE, Statistics.Property.SERVICE_TIME));
        System.out.println("Mean zone LOS: " + Statistics.calculateAverage(departedPatients, Statistics.Stage.ZONE, Statistics.Property.LOS));
    }
}
