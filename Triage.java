
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
    public void departServiceStation(Event currentEvent) {
        Patient patient = currentEvent.patient;
        double currentTime = currentEvent.eventTime;

        setPatientDepartureTime(patient, currentTime);
        departedPatients.add(patient);

        activeTreatments--;
        busyBeds--;

        sendToAppropriateNextStation(currentEvent);

        // If patients are waiting for beds, move one into bed
        if (!queue.isEmpty()) {
            Patient next = queue.poll();
            busyBeds++;
            waitingForStaff.add(next);
            if (debug == 1) {
                System.out.println("[Triage] Patient " + next.id + " got bed after departure @T: " + currentTime);
            }
        }

        // Fill available staff slots
        attemptToStartTreatmentForAll(currentTime);
    }

    @Override
    protected void sendToAppropriateNextStation(Event currentEvent) {
        int ESI = currentEvent.patient.ESILevel;
        Zone targetZone;

        // ESI 1
        if (ESI == 1) {
            targetZone = simulator.eruZone;
        }

        // ESI 2
        else if (ESI == 2) targetZone = simulator.redZone;    // 70%



        // ESI 3
        else if (ESI == 3) {
            double r = Math.random();
            if (r < 0.33) {
                targetZone = simulator.redZone;    // 30%
            } else {
                targetZone = simulator.greenZone;  // 70%
            }
        }

        // ESI 4
        else if (ESI == 4) {
            double r = Math.random();
            if (r < 0.2) {
                targetZone = simulator.greenZone;      // 20%
            } else {
                targetZone = simulator.fastTrackZone;  // 80%
            }
        }

        // ESI 5
        else {
            targetZone = simulator.fastTrackZone;
        }

        targetZone.queuePatientFromAnotherStation(currentEvent);
    }

}
