import java.util.*;

public class SortNurse extends ServiceStation {
    private Registration registration;
    private Zone eruZone;
    public PriorityQueue<Patient> sortNQueue;

    public SortNurse(int queueCapacity, Registration registration, Zone eruZone, PriorityQueue<Event> eventList) {
        super("SortNurse", 10.0, 1.0, eventList);
        this.registration = registration;
        this.eruZone = eruZone;
        this.sortNQueue = this.queue;
    }

    @Override
    protected void setPatientArrivalTime(Patient patient, double time) {
        patient.sortingAT = time;
    }

    @Override
    protected void setPatientDepartureTime(Patient patient, double time) {
        patient.sortingDT = time;
    }

    @Override
    protected void setPatientProcessingTime(Patient patient, double time) {
        patient.sortingPT = time;
    }

    @Override
    protected Event.EventType getDepartureEventType() {
        return Event.EventType.sortDeparture;
    }

    @Override
    protected void processPatientDeparture(Event currentEvent) {
        sendToAppropriateDepartment(currentEvent);
    }

    @Override
    protected Statistics.Stage getStatisticsStage() {
        return Statistics.Stage.SORTING;
    }

    public void departSortingNurse(Event currentEvent) {
        departServiceStation(currentEvent);
    }

    public void sendToAppropriateDepartment(Event currentEvent) {
        double esi = currentEvent.patient.ESILevel;
        double rand = Math.random();

        if (esi == 1 && rand < 0.95) {
            eruZone.addPatient(currentEvent);
        } else if (esi == 2 && rand < 0.10) {
            eruZone.addPatient(currentEvent);
        } else {
            registration.addPatient(currentEvent);
        }
    }
}
