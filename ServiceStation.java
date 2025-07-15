import java.util.*;

public abstract class ServiceStation {
    protected int debug = 1;
    protected Patient currentPatient;
    protected boolean isOccupied;
    protected double meanServiceTime;
    protected double serviceStdDev;
    protected PriorityQueue<Patient> queue;
    protected List<Patient> departedPatients;
    protected PriorityQueue<Event> eventList;
    protected String stationName;

    public ServiceStation(String stationName, double meanServiceTime, double serviceStdDev, PriorityQueue<Event> eventList) {
        this.stationName = stationName;
        this.meanServiceTime = meanServiceTime;
        this.serviceStdDev = serviceStdDev;
        this.queue = new PriorityQueue<>(Comparator.comparingDouble(p -> p.ESILevel));
        this.departedPatients = new ArrayList<>();
        this.currentPatient = null;
        this.isOccupied = false;
        this.eventList = eventList;
    }

    public void addPatient(Event currentEvent) {
        queue.add(currentEvent.patient);
        setPatientArrivalTime(currentEvent.patient, currentEvent.eventTime);
        setPatientDepartureTime(currentEvent.patient, Double.POSITIVE_INFINITY);
        System.out.println("[" + stationName + "]: Added " + currentEvent.patient.id + " to queue @T: " + currentEvent.eventTime);
        
        if (!isOccupied) {
            Patient patientDepartingNext = queue.poll();
            setPatientProcessingTime(patientDepartingNext, currentEvent.eventTime);
            currentPatient = patientDepartingNext;
            scheduleNextDeparture(currentEvent.eventTime, patientDepartingNext);
        }
    }

    protected void scheduleNextDeparture(double currentTime, Patient patient) {
        double serviceTime = Utils.getNormal(meanServiceTime, serviceStdDev);
        double nextDepartureTime = currentTime + serviceTime;
        eventList.add(new Event(nextDepartureTime, getDepartureEventType(), patient));
        isOccupied = true;
        if (debug == 1) {
            System.out.println("[" + stationName + "]: Next departure: " + nextDepartureTime);
        }
    }

    public void departPatient(Event currentEvent) {
        if (currentPatient != currentEvent.patient) {
            throw new IllegalStateException("[" + stationName + "-ERROR]: Got " + currentEvent.patient.id + " [!=] Expected " + currentPatient.id);
        }
        if (debug == 1) {
            System.out.println(currentEvent.patient.id + " DP_" + stationName.toLowerCase() + ": " + currentEvent.eventTime);
        }
        
        processPatientDeparture(currentEvent);
        setPatientDepartureTime(currentPatient, currentEvent.eventTime);
        departedPatients.add(currentPatient);
        isOccupied = false;
        currentPatient = null;
    }

    public void printQuickStats() {
        System.out.println("\n[" + stationName + "]: Quick Stats");
        System.out.println("Total patients processed: " + departedPatients.size());
        System.out.println("Current queue size: " + queue.size());
        System.out.println("Mean " + stationName.toLowerCase() + " waiting time: " + Statistics.calculateAverage(departedPatients, getStatisticsStage(), Statistics.Property.WAITING_TIME));
        System.out.println("Mean " + stationName.toLowerCase() + " service time: " + Statistics.calculateAverage(departedPatients, getStatisticsStage(), Statistics.Property.SERVICE_TIME));
        System.out.println("Mean " + stationName.toLowerCase() + " LOS: " + Statistics.calculateAverage(departedPatients, getStatisticsStage(), Statistics.Property.LOS));
    }

    protected abstract void setPatientArrivalTime(Patient patient, double time);
    protected abstract void setPatientDepartureTime(Patient patient, double time);
    protected abstract void setPatientProcessingTime(Patient patient, double time);
    protected abstract Event.EventType getDepartureEventType();
    protected abstract void processPatientDeparture(Event currentEvent);
    protected abstract Statistics.Stage getStatisticsStage();
}
