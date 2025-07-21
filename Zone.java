import java.util.*;

public class Zone extends ServiceStation {
    public List<Patient> zoneDepartedPatients;
    private List<Patient> edDisposedPatients;
    private Simulator.StationName zoneName;

    //beds and staff treatment handling
    protected Queue<Patient> waitingForStaff;  // patients admitted to a bed but waiting for staff
    protected int maxStaffAvailable;
    protected int activeTreatments = 0;
    protected int busyBeds = 0; //occupied beds

    public Zone(Simulator.StationName zoneName, Simulator simulator) {
        super(zoneName, 4, 1.0, getZoneCapacity(zoneName), simulator);
        this.edDisposedPatients = simulator.edDisposedPatients;
        this.zoneName = zoneName;
        this.zoneDepartedPatients = this.departedPatients;
        this.waitingForStaff = new LinkedList<>();
    }

    private static int getZoneCapacity(Simulator.StationName zoneName) {
        switch (zoneName) {
            case ERU: return 14;
            case FAST_TRACK: return 19;
            case RED: return 34;
            case GREEN: return 10;
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
    public ServiceStation getPrecedingStation() {
        return simulator.triage;
    }
    @Override
    protected void sendToAppropriateNextStation(Event currentEvent) {
        if (!currentEvent.patient.isCountedDisposed) {
            simulator.addDisposedPatient(currentEvent.patient);
        }
    }

    public void setStaffAvailable(int staffCount) {
        this.maxStaffAvailable = staffCount;
    }

    public void queuePatientFromAnotherStation(Event currentEvent) {
        Patient patient = currentEvent.patient;
        double currentTime = currentEvent.eventTime;

        setPatientArrivalTime(patient, currentTime); // optional if Zone-specific time needed
        patient.currentStationName = stationName;
        totalArrivals++;
        arrivedPatients.add(patient);

        // if bed is available
        if (busyBeds < numBeds) {
            busyBeds++;
            waitingForStaff.add(patient);
        } else {
            queue.add(patient);
        }

        // attempt to begin treatment if staff is available
        attemptToStartTreatmentForAll(currentTime);
    }

    @Override
    public void addPatient(Event currentEvent) {
        Patient patient = currentEvent.patient;
        double currentTime = currentEvent.eventTime;

        setPatientArrivalTime(patient, currentTime);
        patient.currentStationName = this.stationName;

        totalArrivals++;
        arrivedPatients.add(patient); 


        // admit if a bed is available
        if (busyBeds < numBeds) {
            busyBeds++;
            waitingForStaff.add(patient);
            attemptToStartTreatment(patient, currentTime);
        } else {
            //add to queue if there are no beds
            queue.add(patient);

            if (debug == 1) {
                System.out.println("[" + stationName + "] Patient " + patient.id + " queued for bed @T: " + currentTime);
            }
        }
    }

    //start treatment
    private void attemptToStartTreatment(Patient patient, double currentTime) {
        if (activeTreatments < maxStaffAvailable) {

            //begin treatment
            waitingForStaff.remove(patient);
            setPatientProcessingTime(patient, currentTime);

            double serviceTime = Utils.getNormal(meanServiceTime, serviceStdDev);
            double nextDeparture = currentTime + serviceTime;
            eventList.add(new Event(nextDeparture, getDepartureEventType(), patient));
            activeTreatments++;

            if (debug == 1) {
                System.out.println("[" + stationName + "]: Started treatment for " + patient.id + " @T: " + currentTime + ", departs @T: " + nextDeparture);
            }
        }
    }

    //fill staff slots from queue
    public void attemptToStartTreatmentForAll(double currentTime) {
        while (!waitingForStaff.isEmpty() && activeTreatments < maxStaffAvailable) {
            Patient next = waitingForStaff.poll();
            attemptToStartTreatment(next, currentTime);
        }
    }

    // free bed + staff, and treat next
    @Override
    public void departServiceStation(Event currentEvent) {
        Patient patient = currentEvent.patient;
        double currentTime = currentEvent.eventTime;

        // unrealistic to assume ESI 1 patients are dying while waiting since treatment should immediately start
        if (patient.ESILevel >= 1 && patient.ESILevel <= 3) {
            double baseRisk;
            if (patient.ESILevel == 1) baseRisk = 0.015;
            else if (patient.ESILevel == 2) baseRisk = 0.008;
            else baseRisk = 0.002;

            double timeInTreatment = currentTime - patient.zonePT;
            double waitBeforeTreatment = patient.zonePT - patient.zoneAT;

            double treatmentFactor = Math.min(1.0, timeInTreatment / 150.0);  // 2.5 hours
            double delayFactor = Math.min(1.0, waitBeforeTreatment / 240.0); // 4 hours

            double combinedRisk = baseRisk * (0.6 * treatmentFactor + 0.4 * delayFactor);

            if (Math.random() < combinedRisk) {
                patient.died = true;
                patient.deathTime = currentTime;
                activeTreatments--;
                busyBeds--;
                if (!patient.isCountedDisposed) simulator.addDisposedPatient(patient);
                if (debug == 1) {
                    System.out.printf("[Death] %d (ESI %d) died during treatment\n",
                            patient.id, patient.ESILevel, stationName, currentTime, combinedRisk);
                }
                return;
            }
        }



        setPatientDepartureTime(patient, currentTime);
        departedPatients.add(patient);

        activeTreatments--;
        busyBeds--;

        sendToAppropriateNextStation(currentEvent);

        // if patients are waiting for beds, move one into bed
        if (!queue.isEmpty()) {
            Patient next = queue.poll();
            busyBeds++;
            waitingForStaff.add(next);
            if (debug == 1) {
                System.out.println("[" + stationName + "] Patient " + next.id + " got bed after departure @T: " + currentTime);
            }
        }

        //try to fill any available staff slots
        attemptToStartTreatmentForAll(currentTime);
    }

    @Override
    protected double getPatientArrivalTime(Patient patient) {
        return patient.zoneAT;
    }

    public int countDeaths() {
        int deaths = 0;
        for (Patient p : edDisposedPatients) {
            if (p.died && p.currentStationName == this.zoneName) {
                deaths++;
            }
        }
        return deaths;
    }

    public int countLWBS() {
        int count = 0;
        for (Patient p : edDisposedPatients) {
            if (p.hasLWBS && p.currentStationName == this.zoneName) {
                count++;
            }
        }
        return count;
    }

    public void printQuickStats() {
        super.printQuickStats();
        System.out.println("Total deaths in zone: " + countDeaths());
        System.out.println("Avg deaths per day: " + (countDeaths() / (double) simulator.numDays));
        System.out.println("Total LWBS in zone: " + countLWBS());
        System.out.println("Avg LWBS per day: " + (countLWBS() / (double) simulator.numDays));
        System.out.println("Patients in bed waiting for staff: " + waitingForStaff.size());
        System.out.println("Active treatments: " + activeTreatments + "/" + maxStaffAvailable);
        System.out.println("Busy beds: " + busyBeds + "/" + numBeds);
    }
}
