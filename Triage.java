
public class Triage extends Zone {
    public Triage(Simulator simulator) {
        super(Simulator.StationName.TRIAGE, simulator);
        this.setStaffAvailable(3);
    }

    @Override
    protected void setPatientArrivalTime(Patient p, double t) {
        p.triageAT = t;
    }

    @Override
    protected void setPatientProcessingTime(Patient p, double t) {
        p.triagePT = t;
    }

    @Override
    protected void setPatientDepartureTime(Patient p, double t) {
        p.triageDT = t;
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

            else if (ESI == 4){
                double r = Math.random();
                if(r<0.60){
                    targetZone = simulator.greenZone;
                } else targetZone = simulator.fastTrackZone;
            }

                //fast track
            else targetZone = simulator.fastTrackZone;

        targetZone.queuePatientFromAnotherStation(currentEvent);
    }
}
