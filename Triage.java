import java.util.*;

public class Triage extends ServiceStation {
    public PriorityQueue<Patient> triageQueue;
    public List<Patient> triagedPatients;
    private Zone eruZone;
    private Zone redZone;
    private Zone greenZone;
    private Zone fastTrackZone;

    public Triage(int queueCapacity, Zone eruZone, Zone redZone, Zone greenZone, Zone fastTrackZone, PriorityQueue<Event> eventList) {
        super("Triage", 10.0, 5.0, eventList);
        this.triageQueue = this.queue;
        this.triagedPatients = this.departedPatients;
        this.eruZone = eruZone;
        this.redZone = redZone;
        this.greenZone = greenZone;
        this.fastTrackZone = fastTrackZone;
    }

    @Override
    protected void setPatientArrivalTime(Patient patient, double time) {
        patient.triageAT = time;
    }

    @Override
    protected void setPatientDepartureTime(Patient patient, double time) {
        patient.triageDT = time;
    }

    @Override
    protected void setPatientProcessingTime(Patient patient, double time) {
        patient.triagePT = time;
    }

    @Override
    protected Event.EventType getDepartureEventType() {
        return Event.EventType.triageDeparture;
    }

    @Override
    protected void processPatientDeparture(Event currentEvent) {
        sendToAppropriateDepartment(currentEvent);
    }

    @Override
    protected Statistics.Stage getStatisticsStage() {
        return Statistics.Stage.TRIAGE;
    }

    public void departTriage(Event currentEvent) {
        departPatient(currentEvent);
    }

    private void sendToAppropriateDepartment(Event currentEvent) {
        String acuity = currentEvent.patient.acuity;
        double rand = Math.random();
        if (acuity.equals("High")) {
            eruZone.addPatient(currentEvent);
        } else if (acuity.equals("Medium")) {
            redZone.addPatient(currentEvent);
        } else if (acuity.equals("Low") && rand > 0.75) {
            greenZone.addPatient(currentEvent);
        } else {
            fastTrackZone.addPatient(currentEvent);
        }
    }
}
