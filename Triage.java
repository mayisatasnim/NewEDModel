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
    public ServiceStation getPrecedingStation() {
        return simulator.registration;
    }
    @Override
    protected void sendToAppropriateNextStation(Event currentEvent) {
        int ESI = currentEvent.patient.ESILevel;

        //assign zone based on acuity and esi
        Zone targetZone;

        //high
        if (ESI == 1) targetZone = simulator.eruZone;

            //moderate and low acuity
        else if (ESI == 2) targetZone = simulator.redZone;

            //divide esi 3 patients between red and green zone
        else if (ESI == 3){
            double r = Math.random();
            if(r<0.70){
                targetZone = simulator.redZone;
            } else targetZone = simulator.greenZone;
        }

        else if (ESI == 4) targetZone = simulator.greenZone;

            //fast track
        else targetZone = simulator.fastTrackZone;

        targetZone.addPatient(currentEvent);

    }
}
