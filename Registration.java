import java.util.*;

public class Registration extends ServiceStation {
    private Triage triage;
    public PriorityQueue<Patient> regQueue;

    public Registration(Triage triage, PriorityQueue<Event> eventList) {
        super("Registration", 8.0, 3.0, eventList);
        this.triage = triage;
        this.regQueue = this.queue;
    }

    @Override
    protected void setPatientArrivalTime(Patient patient, double time) {
        patient.registrationAT = time;
    }

    @Override
    protected void setPatientDepartureTime(Patient patient, double time) {
        patient.registrationDT = time;
    }

    @Override
    protected void setPatientProcessingTime(Patient patient, double time) {
        patient.registrationPT = time;
    }

    @Override
    protected Event.EventType getDepartureEventType() {
        return Event.EventType.registerDeparture;
    }

    @Override
    protected void processPatientDeparture(Event currentEvent) {
        triage.addPatient(currentEvent);
    }

    @Override
    protected Statistics.Stage getStatisticsStage() {
        return Statistics.Stage.REGISTRATION;
    }

    public void departRegistration(Event currentEvent) {
        departServiceStation(currentEvent);
    }
}
