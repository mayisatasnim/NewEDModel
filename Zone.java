import java.util.*;

public class Zone extends ServiceStation {
    public PriorityQueue<Patient> zoneQueue;
    public List<Patient> zoneDepartedPatients;
    private List<Patient> edDisposedPatients;
    private Simulator.zoneName zoneName;



    //admitted but waiting for treatment
    private Queue<Patient> waitingForStaff;  // patients admitted to a bed but waiting for staff
    private int maxStaffAvailable;
    private int activeTreatments = 0;


    private Simulator simulator;


    public Zone(Simulator.zoneName zoneName, List<Patient> edDisposedPatients, PriorityQueue<Event> eventList, Simulator sim) {
        super("Zone-" + zoneName, 4, 1.0, getZoneCapacity(zoneName), eventList);
        this.edDisposedPatients = edDisposedPatients;
        this.simulator = sim;

        this.zoneName = zoneName;
        this.zoneQueue = this.queue;
        this.zoneDepartedPatients = this.departedPatients;
        this.waitingForStaff = new LinkedList<>();
    }

    private static int getZoneCapacity(Simulator.zoneName zoneName) {
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
    protected void processPatientDeparture(Event currentEvent) {
        if (!currentEvent.patient.isCountedDisposed) {
            simulator.addDisposedPatient(currentEvent.patient);
        }
    }

    @Override
    protected Statistics.Stage getStatisticsStage() {
        return Statistics.Stage.ZONE;
    }

    @Override
    public void addPatient(Event currentEvent) {
        Patient patient = currentEvent.patient;
        patient.zoneName = zoneName;

        // checking lwbs for walk-in, non-critical patients
        if (patient.arrivalMode.equals("Walk-in") && patient.ESILevel >= 3) {
            int hostQueueSize = queue.size();
            double doorToProviderTime = currentEvent.eventTime - patient.sortingAT;
            int currentHour = (int) ((currentEvent.eventTime / 60.0) % 24);
            double arrivalRate = Simulator.getArrivalRateByTime(currentHour);


            patient.computeLWBSProbability(hostQueueSize, arrivalRate, doorToProviderTime, currentHour);
            double r = Math.random();
            if (r < patient.LWBSProbability) {
                patient.hasLWBS = true;
                patient.LWBSTime = currentEvent.eventTime;

                if (debug == 1) {
                    System.out.println("[LWBS]" + patient.id + " LEFT WITHOUT BEING SEEN in " + stationName + " @T: " + patient.LWBSTime);
                }

                if (!patient.isCountedDisposed) {
                    simulator.addDisposedPatient(patient);
                }

                return; //patient leaves w/out being added to queue
            }
        }

        if (busyBeds < numBeds) {
            busyBeds++;
            if (activeTreatments < maxStaffAvailable) {
                //start treatment if staff is available
                activeTreatments++;
                setPatientProcessingTime(patient, currentEvent.eventTime);
                scheduleNextDeparture(currentEvent.eventTime, patient);
            } else {
                //in bed but no staff available to treat
                waitingForStaff.add(patient);
            }
        } else {
            //no bed, regular queue
            queue.add(patient);
        }

        totalArrivals++;
        setPatientArrivalTime(patient, currentEvent.eventTime);
        setPatientDepartureTime(patient, Double.POSITIVE_INFINITY);

        if (debug == 1) {
            System.out.println("[" + stationName + "]: Added " + patient.id + " to bed @T: " + currentEvent.eventTime);
        }

    }

    public void departZone(Event currentEvent) {
        departServiceStation(currentEvent);
    }

    @Override
    public void departServiceStation(Event currentEvent) {
        Patient p = currentEvent.patient;

        if (debug == 1) {
            System.out.println(p.id + " DP_" + stationName.toLowerCase() + ": " + currentEvent.eventTime);
        }

        processPatientDeparture(currentEvent);
        setPatientDepartureTime(p, currentEvent.eventTime);
        departedPatients.add(p);

        activeTreatments--;
        busyBeds--;

        //check patients in beds but waiting for staff
        if (!waitingForStaff.isEmpty() && activeTreatments < maxStaffAvailable) {
            Patient next = waitingForStaff.poll();
            activeTreatments++;
            setPatientProcessingTime(next, currentEvent.eventTime);
            scheduleNextDeparture(currentEvent.eventTime, next);
        }

        //admit from main queue if beds available
        if (!queue.isEmpty() && busyBeds < numBeds) {
            Patient nextBedPatient = queue.poll();
            busyBeds++;
            if (activeTreatments < maxStaffAvailable) {
                activeTreatments++;
                setPatientProcessingTime(nextBedPatient, currentEvent.eventTime);
                scheduleNextDeparture(currentEvent.eventTime, nextBedPatient);
            } else {
                waitingForStaff.add(nextBedPatient);
            }
        }
    }

    public void setStaffAvailable(int staffCount) {
        this.maxStaffAvailable = staffCount;
    }



    //check for death in zone
    @Override
    protected void scheduleNextDeparture(double currentTime, Patient patient) {
        double serviceTime = Utils.getNormal(meanServiceTime, serviceStdDev);

        if ((patient.ESILevel == 1) && serviceTime > 360) {
            patient.died = true;
            patient.deathTime = currentTime + 360;

            if (!patient.isCountedDisposed) {
                simulator.addDisposedPatient(patient);
            }


            if (debug == 1) {
                System.out.println("[Death] " + patient.id + " died during treatment in " + stationName + " @T: " + patient.deathTime);
            }
            return; //no normal departure
        }

        double nextDepartureTime = currentTime + serviceTime;
        eventList.add(new Event(nextDepartureTime, getDepartureEventType(), patient));
        if (debug == 1) {
            System.out.println("[" + stationName + "]: Next departure for " + patient.id + ": " + nextDepartureTime);
        }
    }

    @Override
    protected double getPatientArrivalTime(Patient patient) {
        return patient.zoneAT;
    }

    public int countDeaths() {
        int deaths = 0;
        for (Patient p : edDisposedPatients) {
            if (p.died && p.zoneName == this.zoneName) {
                deaths++;
            }
        }
        return deaths;
    }

    public int countLWBS() {
        int count = 0;
        for (Patient p : edDisposedPatients) {
            if (p.hasLWBS && p.zoneName == this.zoneName) {
                count++;
            }
        }
        return count;
    }


    public void printQuickStats(int numDays) {
        System.out.println("\n[Zone-" + zoneName + "]: Quick Stats");
        System.out.println("Days simulated: " + numDays);
        System.out.println("Total arrivals: " + totalArrivals);
        System.out.println("Avg arrivals per day: " + (totalArrivals / (double) numDays));
        System.out.println("Total processed: " + departedPatients.size());
        System.out.println("Avg processed per day: " + (departedPatients.size() / (double) numDays));
        System.out.println("Current Queue size [waiting]: " + queue.size());
        System.out.println("Mean zone waiting time: " + Statistics.calculateMean(departedPatients, Statistics.Stage.ZONE, Statistics.Property.WAITING_TIME));
        System.out.println("Mean zone service time: " + Statistics.calculateMean(departedPatients, Statistics.Stage.ZONE, Statistics.Property.PROCESSING_TIME));
        System.out.println("Total deaths in zone: " + countDeaths());
        System.out.println("Avg deaths per day: " + (countDeaths() / (double) numDays));
        System.out.println("Total LWBS in zone: " + countLWBS());
        System.out.println("Avg LWBS per day: " + (countLWBS() / (double) numDays));
        System.out.println("Patients in bed waiting for staff: " + waitingForStaff.size());
        System.out.println("Active treatments: " + activeTreatments + "/" + maxStaffAvailable);
    }


}
