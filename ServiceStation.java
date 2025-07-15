import java.util.*;

public abstract class ServiceStation extends Metrics {
    protected int debug = 0;
    protected Patient currentPatient;
    protected boolean isOccupied;
    protected double meanServiceTime;
    protected double serviceStdDev;
    protected PriorityQueue<Patient> queue;
    protected List<Patient> departedPatients;
    protected List<Patient> arrivedPatients;
    protected PriorityQueue<Event> eventList;
    protected String stationName;


    public ServiceStation(String stationName, double meanServiceTime, double serviceStdDev, PriorityQueue<Event> eventList) {
        super(stationName);
        this.stationName = stationName;
        setServiceTime(meanServiceTime, serviceStdDev);
        this.queue = new PriorityQueue<>(Comparator.comparingDouble(p -> p.ESILevel));
        this.departedPatients = new ArrayList<>();
        this.currentPatient = null;
        this.isOccupied = false;
        this.eventList = eventList;
    }

    public void setServiceTime(double meanServiceTime, double serviceStdDev) {
        this.meanServiceTime = meanServiceTime;
        this.serviceStdDev = serviceStdDev;
    }

    public void addPatient(Event currentEvent) {
        queue.add(currentEvent.patient);
        totalArrivals++;
        setPatientArrivalTime(currentEvent.patient, currentEvent.eventTime);
        setPatientDepartureTime(currentEvent.patient, Double.POSITIVE_INFINITY);
        if (debug == 1) {
            System.out.println("[" + stationName + "]: Added " + currentEvent.patient.id + " to queue @T: " + currentEvent.eventTime);
        }

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

    public void departServiceStation(Event currentEvent) {
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
        computeMetrics();
        System.out.println("\n[" + stationName + "]: Quick Stats");
        System.out.println("Total arrived: " + totalArrivals);
        System.out.println("Total processed: " + departedPatients.size());
        System.out.println("Current Queue size[waiting]: " + queue.size());
        System.out.println("Mean " + stationName.toLowerCase() + " Real waiting time: " + realMeanWaitingTime);
        System.out.println("Mean " + stationName.toLowerCase() + " Real service time: " + realMeanServiceTime);
        System.out.println("Expected " + stationName.toLowerCase() + " Mean Service time: " + meanServiceTime);
        System.out.println("Mean " + stationName.toLowerCase() + " Real LOS[ResponseTime]: " + realResponseTime);
        System.out.println("Mean " + stationName.toLowerCase() + " Real Inter-Arrival Time: " + realMeanInterArrivalTime);
    }

    public void computeMetrics() {
        realMeanWaitingTime = Statistics.calculateMean(departedPatients, getStatisticsStage(), Statistics.Property.WAITING_TIME);
        realMeanServiceTime = Statistics.calculateMean(departedPatients, getStatisticsStage(), Statistics.Property.PROCESSING_TIME);
        realResponseTime = Statistics.calculateMean(departedPatients, getStatisticsStage(), Statistics.Property.RESPONSE_TIME);
        realMeanInterArrivalTime = Statistics.calculateMean(arrivedPatients, getStatisticsStage(), Statistics.Property.INTER_ARRIVAL_TIME);
        totalProcessed = departedPatients.size(); // (X) - Throughput
        currentQueueSize = queue.size(); // (NQ) - Current Queue Size
        realServiceRate = 1.0 / realMeanServiceTime;
        utilization = realArrivalRate / realServiceRate; // Utilization (ρ) = λ / μ
    }

    protected abstract void setPatientArrivalTime(Patient patient, double time);
    protected abstract void setPatientDepartureTime(Patient patient, double time);
    protected abstract void setPatientProcessingTime(Patient patient, double time);
    protected abstract Event.EventType getDepartureEventType();
    protected abstract void processPatientDeparture(Event currentEvent);
    protected abstract Statistics.Stage getStatisticsStage();
}
