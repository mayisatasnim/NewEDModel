import java.util.*;

public class Registration {
    private int debug = 1;

    private Patient currentPatient;
    private boolean isOccupied;
    private final double meanRegTime = 8.0;
    private final double regStdDev = 3.0;
    private Triage triage;
    public PriorityQueue<Patient> regQueue;
    public List<Patient> departedPatients;
    PriorityQueue<Event> eventList;


    public Registration(int queueCapacity, Triage triage, PriorityQueue<Event> eventList) {
        this.regQueue = new PriorityQueue<>(Comparator.comparingDouble(p -> p.ESILevel));
        this.departedPatients = new ArrayList<Patient>();
        this.currentPatient = null;
        this.isOccupied = false;
        this.triage = triage;
        this.eventList = eventList;
    }

    public void addPatient(Event currentEvent) {
            regQueue.add(currentEvent.patient);
            currentEvent.patient.registrationAT = currentEvent.eventTime;
            currentEvent.patient.registrationDT = Double.POSITIVE_INFINITY;
            System.out.println("[Registration]: Added " + currentEvent.patient.id + " to RegQueue @T: " + currentEvent.eventTime);
            if(!isOccupied){
                Patient patientDepartingNext = regQueue.poll();
                patientDepartingNext.registrationPT = currentEvent.eventTime;
                currentPatient = patientDepartingNext;
                scheduleNextDeparture(currentEvent.eventTime,patientDepartingNext);
            }
    }

    public void scheduleNextDeparture(double currentTime, Patient patient){
        double serviceTime= Utils.getNormal(meanRegTime,regStdDev);
        double nextDepartureTime = currentTime + serviceTime;
        eventList.add(new Event(nextDepartureTime, Event.EventType.registerDeparture,patient));
        isOccupied = true;
        if(debug == 1){ System.out.println("[Registration]: Next regDT: " + nextDepartureTime);}

    }

    public void departRegistration(Event currentEvent) {
        if(currentPatient != currentEvent.patient) {
            throw new IllegalStateException("[Registration-ERROR]: Got " + currentEvent.patient.id + " AT@ " +currentEvent.patient.registrationAT + " [!=] \n Expected " + currentPatient.id+ " AT@ " + currentPatient.registrationAT);
        }
        if(debug == 1) {System.out.println(currentEvent.patient.id + " DP_reg: " +currentEvent.eventTime);}
        // process departure
        triage.addPatient(currentEvent);
        currentPatient.registrationDT = currentEvent.eventTime;
        departedPatients.add(currentPatient);
        isOccupied = false;
        currentPatient = null;
    }

    public void printQuickStats() {
        System.out.println("\n[Registration]: Quick Stats");
        System.out.println("Total patients registered: " + departedPatients.size());
        System.out.println("Current RegQueue size[if end of day indicates unprocessed patients]: " + regQueue.size());
        System.out.println("Mean Reg waiting time: " + Statistics.calculateAverage(departedPatients, Statistics.Stage.REGISTRATION, Statistics.Property.WAITING_TIME));
        System.out.println("Mean Reg service time: " + Statistics.calculateAverage(departedPatients, Statistics.Stage.REGISTRATION, Statistics.Property.SERVICE_TIME));
        System.out.println("Mean Reg LOS: " + Statistics.calculateAverage(departedPatients, Statistics.Stage.REGISTRATION, Statistics.Property.LOS));
    }
}
