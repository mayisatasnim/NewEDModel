import java.util.*;

public class Triage extends ServiceStation {
    public PriorityQueue<Patient> triageQueue;
    public List<Patient> triagedPatients;
    private Zone eruZone;
    private Zone redZone;
    private Zone greenZone;
    private Zone fastTrackZone;

    public Triage(Zone eruZone, Zone redZone, Zone greenZone, Zone fastTrackZone, PriorityQueue<Event> eventList) {
        super("Triage", 10.0, 5.0, 3, eventList);
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
        departServiceStation(currentEvent);
    }

    @Override
    protected double getPatientArrivalTime(Patient patient) {
        return patient.triageAT;
    }

    private void sendToAppropriateDepartment(Event currentEvent) {
        // use bell curve to send patients to appropriate zones based on their acuity
        int ESI = currentEvent.patient.ESILevel;

        //assign zone based on acuity and esi
        Zone targetZone;
        if (ESI == 1) targetZone = eruZone;
        else if (ESI == 2) targetZone = redZone;
        else if (ESI == 3) targetZone = redZone;
        else if (ESI == 4) targetZone = greenZone;
        else targetZone = fastTrackZone;


        targetZone.addPatient(currentEvent);



//        double zoneAssignmentAccuracy = Utils.getNormal(3.0, 1.0);
//        boolean isAccurate = zoneAssignmentAccuracy >= 2 && zoneAssignmentAccuracy <= 4;
//        Zone targetZone;
//        Zone fallbackZone;

//        switch (acuity) {
//            case "ERU":
//            targetZone = eruZone;
//            fallbackZone = redZone;
//            break;
//            case "RED":
//            targetZone = redZone;
//            fallbackZone = greenZone;
//            break;
//            case "GREEN":
//            targetZone = greenZone;
//            fallbackZone = fastTrackZone;
//            break;
//            default:
//            targetZone = fastTrackZone;
//            fallbackZone = null;
//        }
//
//        if (isAccurate || fallbackZone == null) {
//            targetZone.addPatient(currentEvent);
//        } else {
//            fallbackZone.addPatient(currentEvent);
//        }


    }
}
