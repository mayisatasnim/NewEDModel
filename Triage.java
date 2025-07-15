import java.util.*;

public class Triage {
    private int debug = 1;

    private Patient currentPatient;
    private boolean isOccupied;
    private final double meanTriageTime = 10.0;
    private final double triageStdDev = 5.0;
    public PriorityQueue<Patient> triageQueue;
    public List<Patient> triagedPatients;
    PriorityQueue<Event> eventList;
    Zone eruZone;
    Zone redZone;
    Zone greenZone;
    Zone fastTrackZone;

    public Triage(int queueCapacity, Zone eruZone, Zone redZone, Zone greenZone, Zone fastTrackZone, PriorityQueue<Event> eventList) {
        this.triageQueue = new PriorityQueue<>(Comparator.comparingDouble(p -> p.ESILevel));
        this.triagedPatients = new ArrayList<Patient>();
        this.currentPatient = null;
        this.isOccupied = false;
        this.eventList = eventList;
        this.eruZone = eruZone;
        this.redZone = redZone;
        this.greenZone = greenZone;
        this.fastTrackZone = fastTrackZone;
    }

    public void addPatient(Event currentEvent) {
        triageQueue.add(currentEvent.patient);
        currentEvent.patient.triageAT = currentEvent.eventTime;
        currentEvent.patient.triageDT = Double.POSITIVE_INFINITY;
        System.out.println("[Triage]: Added " + currentEvent.patient.id + " to triageQueue @T: " + currentEvent.eventTime);
        if (!isOccupied) {
            Patient patientDepartingNext = triageQueue.poll();
            patientDepartingNext.triagePT = currentEvent.eventTime;
            currentPatient = patientDepartingNext;
            scheduleNextDeparture(currentEvent.eventTime, patientDepartingNext);
        }
    }

    public void scheduleNextDeparture(double currentTime, Patient patient) {
        double serviceTime = Utils.getNormal(meanTriageTime, triageStdDev);
        double nextDepartureTime = currentTime + serviceTime;
        eventList.add(new Event(nextDepartureTime, Event.EventType.triageDeparture, patient));
        isOccupied = true;
        if (debug == 1) {
            System.out.println("[Triage]: Next triageDT: " + nextDepartureTime);
        }
    }

    public void departTriage(Event currentEvent) {
        if (currentPatient != currentEvent.patient) {
            throw new IllegalStateException("[Triage-ERROR]: Got " + currentEvent.patient.id + " AT@ " + currentEvent.patient.triageAT + " [!=] \n Expected " + currentPatient.id + " AT@ " + currentPatient.triageAT);
        }
        if (debug == 1) {
            System.out.println(currentEvent.patient.id + " DP_triage: " + currentEvent.eventTime);
        }
        // process departure
        sendToAppropriateDepartment(currentEvent);
        currentPatient.triageDT = currentEvent.eventTime;
        triagedPatients.add(currentPatient);
        isOccupied = false;
        currentPatient = null;
    }

    public void printQuickStats() {
        System.out.println("\n[Triage]: Quick Stats");
        System.out.println("Total patients triaged: " + triagedPatients.size());
        System.out.println("Current triageQueue size[if end of day indicates unprocessed patients]: " + triageQueue.size());
        System.out.println("Mean triage waiting time: " + Statistics.calculateAverage(triagedPatients, Statistics.Stage.TRIAGE, Statistics.Property.WAITING_TIME));
        System.out.println("Mean triage service time: " + Statistics.calculateAverage(triagedPatients, Statistics.Stage.TRIAGE, Statistics.Property.SERVICE_TIME));
        System.out.println("Mean triage LOS: " + Statistics.calculateAverage(triagedPatients, Statistics.Stage.TRIAGE, Statistics.Property.LOS));
    }

    private void sendToAppropriateDepartment(Event currentEvent) {
        String acuity = currentEvent.patient.acuity;
        double rand = Math.random();
        if (acuity.equals("High")) {
            eruZone.addPatient(currentEvent);
        } else if (acuity.equals("Medium")) {
            redZone.addPatient(currentEvent);
        } else if (acuity.equals("Low") && rand > 0.75) { // 25% to go to fast track
            greenZone.addPatient(currentEvent);
        } else {
            fastTrackZone.addPatient(currentEvent);
        }
    }
}
