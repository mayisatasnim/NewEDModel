public class Triage extends ServiceStation {
    public Triage(Simulator simulator) {
        super(Simulator.StationName.TRIAGE, 10.0, 5.0, 3, simulator);
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
    protected double getPatientArrivalTime(Patient patient) {
        return patient.triageAT;
    }
    @Override
    protected void sendToAppropriateNextStation(Event currentEvent) {
        // use bell curve to send patients to appropriate zones based on their acuity
        int ESI = currentEvent.patient.ESILevel;

        //assign zone based on acuity and esi
        Zone targetZone;
        if (ESI == 1) targetZone = simulator.eruZone;
        else if (ESI == 2) targetZone = simulator.redZone;
        else if (ESI == 3) targetZone = simulator.redZone;
        else if (ESI == 4) targetZone = simulator.greenZone;
        else targetZone = simulator.fastTrackZone;


        targetZone.addPatient(currentEvent);

    }
}
