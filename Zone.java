import java.util.*;

public class Zone extends ServiceStation {
    public List<Patient> zoneDepartedPatients;
    private List<Patient> edDisposedPatients;
    private Simulator.StationName zoneName;



    //admitted but waiting for treatment
    private Queue<Patient> waitingForStaff;  // patients admitted to a bed but waiting for staff
    private int maxStaffAvailable;
    private int activeTreatments = 0;

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
            case FAST_TRACK: return 43;
            case RED: return 29;
            case GREEN: return 32;
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
    protected void sendToAppropriateNextStation(Event currentEvent) {
        if (!currentEvent.patient.isCountedDisposed) {
            simulator.addDisposedPatient(currentEvent.patient);
        }
    }

    public void setStaffAvailable(int staffCount) {
        this.maxStaffAvailable = staffCount;
    }

    // @Override
    // protected void scheduleNextDeparture(double currentTime) {
    //     if ((patient.ESILevel == 1) && serviceTime > 360) {
    //         patient.died = true;
    //         patient.deathTime = currentTime + 360;

    //         if (!patient.isCountedDisposed) {
    //             simulator.addDisposedPatient(patient);
    //         }


    //         if (debug == 1) {
    //             System.out.println("[Death] " + patient.id + " died during treatment in " + stationName + " @T: " + patient.deathTime);
    //         }
    //         return; //no normal departure
    //     }

    //     double nextDepartureTime = currentTime + serviceTime;
    //     eventList.add(new Event(nextDepartureTime, getDepartureEventType(), patient));
    //     if (debug == 1) {
    //         System.out.println("[" + stationName + "]: Next departure for " + patient.id + ": " + nextDepartureTime);
    //     }
    // }

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
    }


}
