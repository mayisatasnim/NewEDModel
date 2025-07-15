import java.util.*;

public class SortNurse {
    private int debug = 1;

    private Patient currentPatient;
    private double nextDepartureTime;
    public double currentTime;
    private boolean isOccupied;
    private final double meanSortTime = 4.0;
    private final double sortStdDev = 1.0;

    private boolean sendToERU;
    private Registration registration;
    public PriorityQueue<Patient> sortNQueue;

    PriorityQueue<Event> eventList;
    List<Patient> departedPatients;


    public SortNurse(int queueCapacity, Registration registration, PriorityQueue<Event> eventList, List<Patient> departedPatients) {
        this.sortNQueue = new PriorityQueue<>(Comparator.comparingDouble(p -> p.ESILevel));
        this.currentPatient = null;
        this.isOccupied = false;
        this.nextDepartureTime = Double.POSITIVE_INFINITY;
        this.sendToERU = false;
        this.registration = registration;
        this.eventList = eventList;
        this.departedPatients = departedPatients;
    }

    public void addPatient(Event currentEvent) {
            sortNQueue.add(currentEvent.patient);
            currentEvent.patient.sortingAT = currentEvent.eventTime;
            currentEvent.patient.sortingDT = Double.POSITIVE_INFINITY;
            System.out.println("[SortNurse]: Added " + currentEvent.patient.id + " to sortQueue @T: " + currentEvent.eventTime);
            if(!isOccupied){
                Patient patientDepartingNext = sortNQueue.poll();
                currentPatient = patientDepartingNext;
                scheduleNextDeparture(currentEvent.eventTime,patientDepartingNext);
            }
    }

    public void scheduleNextDeparture(double currentTime, Patient patient){
        double serviceTime= Utils.getNormal(meanSortTime,sortStdDev);
        double nextDepartureTime = currentTime + serviceTime;
        this.nextDepartureTime = nextDepartureTime;
        eventList.add(new Event(nextDepartureTime, Event.EventType.sortDeparture,patient));
        patient.sortingDT = nextDepartureTime;
        isOccupied = true;
        if(debug == 1){ System.out.println("[SortNurse]: Next srtDT: " + nextDepartureTime);}

    }

    public void departSortingNurse(Event currentEvent) {
        if(currentPatient != currentEvent.patient) {
            throw new IllegalStateException("[SortNurse-ERROR]: Got " + currentEvent.patient.id + " AT@ " +currentEvent.patient.sortingAT + " [!=] \n Expected " + currentPatient.id+ " AT@ " + currentPatient.sortingAT);
        }
        System.out.println(currentEvent.patient.id + " DP_sort: " + currentTime);
        currentEvent.patient.sortingDT = currentTime;
        departedPatients.add(currentEvent.patient);
        isOccupied = false;
        currentPatient = null;
    }
}
