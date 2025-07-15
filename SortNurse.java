import java.util.*;

public class SortNurse {
    private int debug = 1;

    private Patient currentPatient;
    private boolean isOccupied;
    private final double meanSortTime = 4.0;
    private final double sortStdDev = 1.0;
    private Registration registration;
    public PriorityQueue<Patient> sortNQueue;
    public List<Patient> departedPatients;
    PriorityQueue<Event> eventList;


    public SortNurse(int queueCapacity, Registration registration, PriorityQueue<Event> eventList) {
        this.sortNQueue = new PriorityQueue<>(Comparator.comparingDouble(p -> p.ESILevel));
        this.departedPatients = new ArrayList<Patient>();
        this.currentPatient = null;
        this.isOccupied = false;
        this.registration = registration;
        this.eventList = eventList;
    }

    public void addPatient(Event currentEvent) {
            sortNQueue.add(currentEvent.patient);
            currentEvent.patient.sortingAT = currentEvent.eventTime;
            currentEvent.patient.sortingDT = Double.POSITIVE_INFINITY;
            System.out.println("[SortNurse]: Added " + currentEvent.patient.id + " to sortQueue @T: " + currentEvent.eventTime);
            if(!isOccupied){
                Patient patientDepartingNext = sortNQueue.poll();
                patientDepartingNext.sortingPT = currentEvent.eventTime;
                currentPatient = patientDepartingNext;
                scheduleNextDeparture(currentEvent.eventTime,patientDepartingNext);
            }
    }

    public void scheduleNextDeparture(double currentTime, Patient patient){
        double serviceTime= Utils.getNormal(meanSortTime,sortStdDev);
        double nextDepartureTime = currentTime + serviceTime;        
        eventList.add(new Event(nextDepartureTime, Event.EventType.sortDeparture,patient));
        isOccupied = true;
        if(debug == 1){ System.out.println("[SortNurse]: Next srtDT: " + nextDepartureTime);}

    }

    public void departSortingNurse(Event currentEvent) {
        if(currentPatient != currentEvent.patient) {
            throw new IllegalStateException("[SortNurse-ERROR]: Got " + currentEvent.patient.id + " AT@ " +currentEvent.patient.sortingAT + " [!=] \n Expected " + currentPatient.id+ " AT@ " + currentPatient.sortingAT);
        }
        if(debug == 1) {System.out.println(currentEvent.patient.id + " DP_sort: " +currentEvent.eventTime);}
        currentEvent.patient.sortingDT = currentEvent.eventTime;
        departedPatients.add(currentEvent.patient);
        isOccupied = false;
        currentPatient = null;
    }

    public void printQuickStats() {
        System.out.println("\n[SortNurse]: Quick Stats");
        System.out.println("Total patients sorted: " + departedPatients.size());
        System.out.println("Current sortNQueue size[if end of day indicates unprocessed patients]: " + sortNQueue.size());
        System.out.println("Mean sortNurse waiting time: " + Statistics.calculateAverage(departedPatients, Statistics.Stage.SORTING, Statistics.Property.WAITING_TIME));
        System.out.println("Mean sortNurse service time: " + Statistics.calculateAverage(departedPatients, Statistics.Stage.SORTING, Statistics.Property.SERVICE_TIME));
        System.out.println("Mean sortNurse LOS: " + Statistics.calculateAverage(departedPatients, Statistics.Stage.SORTING, Statistics.Property.LOS));
    }
}
