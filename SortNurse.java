import java.util.PriorityQueue;

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
    private final QueueManager queueManager;

    PriorityQueue<Event> eventList;


    public SortNurse(int queueCapacity, Registration registration, PriorityQueue eventList) {
        this.queueManager = new QueueManager(queueCapacity, QueueManager.Stage.Sort);
        this.currentPatient = null;
        this.isOccupied = false;
        this.nextDepartureTime = Double.POSITIVE_INFINITY;
        this.sendToERU = false;
        this.registration = registration;

        this.eventList = eventList;
    }

    public void addPatient(Event currentEvent) {
           // currentEvent.patient.setAcuity(currentEvent.patient.level);
            if(debug == 1){ System.out.println("[SortNurse]: Adding patient to Sort Queue");}
            queueManager.add(currentEvent.patient, currentEvent.eventTime);
            if(!queueManager.isEmpty() && !isOccupied){

                Patient patientDepartingNext = queueManager.pop();
                scheduleNextDeparture(currentEvent.eventTime,patientDepartingNext);

            }
    }

    public void update(){

    }

    public void scheduleNextDeparture(double currentTime, Patient patient){
        double servicetime= Utils.getNormal(4,1);
        double nextDepartureTime = currentTime + servicetime;
        this.nextDepartureTime = nextDepartureTime;
        eventList.add(new Event(nextDepartureTime, Event.EventType.sortDeparture,patient));
        patient.sortingDT = nextDepartureTime;
        isOccupied = true;
        if(debug == 1){ System.out.println("[SortNurse]: Next departure time set for" + nextDepartureTime);}

    }

    public void departSortingNurse(){
        isOccupied = false;
    }


    private boolean checkLWBS(Patient p, double time) {
        if (p.hasLWBS && time >= p.LWBSTime) {
            if (debug == 1) {
                System.out.println("[SortNurse] Patient LWBS at time " + time + ", removing from queue");
            }
            queueManager.remove(p);
            return true;
        }
        return false;
    }

}
