import java.util.*;

public abstract class ServiceStation extends Metrics {
    protected int debug = 0;

    //change for multiple bed support in zones
    protected int numBeds; //servers
    protected int busyBeds = 0; //beds in use

    protected int staffAvailable;
    protected int busyStaff = 0;

    protected double meanServiceTime;
    protected double serviceStdDev;
    protected PriorityQueue<Patient> queue;
    protected List<Patient> departedPatients;
    protected List<Patient> arrivedPatients;
    protected PriorityQueue<Event> eventList;
    protected String stationName;


    public ServiceStation(String stationName, double meanServiceTime, double serviceStdDev, int numBeds, PriorityQueue<Event> eventList) {
        super(stationName);
        this.stationName = stationName;
        setServiceTime(meanServiceTime, serviceStdDev);

        //for multiple bed support
        this.numBeds = numBeds;
        this.busyBeds = 0;


        this.queue = new PriorityQueue<>(Comparator.comparingDouble(p -> p.ESILevel));
        this.departedPatients = new ArrayList<>();
        this.arrivedPatients = new ArrayList<>();
        this.eventList = eventList;
    }

    public void setServiceTime(double meanServiceTime, double serviceStdDev) {
        this.meanServiceTime = meanServiceTime;
        this.serviceStdDev = serviceStdDev;
    }

    public void addPatient(Event currentEvent) {
        Patient patient = currentEvent.patient;
        queue.add(patient);
        totalArrivals++;
        setPatientArrivalTime(patient, currentEvent.eventTime);
        setPatientDepartureTime(patient, Double.POSITIVE_INFINITY);

        if (debug == 1) {
            System.out.println("[" + stationName + "]: Added " + patient.id + " to queue @T: " + currentEvent.eventTime);
        }

        // if available bed
        if (busyBeds < numBeds) {
            Patient nextPatient = queue.poll();
            double waitTime = currentEvent.eventTime - getPatientArrivalTime(nextPatient);
            double startServiceTime = currentEvent.eventTime;

            setPatientProcessingTime(nextPatient, startServiceTime); //when service begins
            scheduleNextDeparture(startServiceTime, nextPatient);
            busyBeds++;
        }

    }

    protected void scheduleNextDeparture(double currentTime, Patient patient) {
        double serviceTime = Utils.getNormal(meanServiceTime, serviceStdDev);
        double nextDepartureTime = currentTime + serviceTime;
        eventList.add(new Event(nextDepartureTime, getDepartureEventType(), patient));
        if (debug == 1) {
            System.out.println("[" + stationName + "]: Next departure: " + nextDepartureTime);
        }
    }

    public void departServiceStation(Event currentEvent) {

        if (debug == 1) {
            System.out.println(currentEvent.patient.id + " DP_" + stationName.toLowerCase() + ": " + currentEvent.eventTime);
        }

        processPatientDeparture(currentEvent);
        setPatientDepartureTime(currentEvent.patient, currentEvent.eventTime);
        departedPatients.add(currentEvent.patient);
        busyBeds--;

        //start service for another patient if queue isn't empty
        if (!queue.isEmpty()) {
            Patient nextPatient = queue.poll();
            setPatientProcessingTime(nextPatient, currentEvent.eventTime);
            scheduleNextDeparture(currentEvent.eventTime, nextPatient);
            busyBeds++;
        }


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
        realServiceRate = (realMeanServiceTime > 0) ? 1.0 / realMeanServiceTime : 0;
        realArrivalRate = (realMeanInterArrivalTime > 0) ? 1.0 / realMeanInterArrivalTime : 0;
        utilization = (realServiceRate > 0) ? realArrivalRate / realServiceRate : 0;  // Utilization (ρ) = λ / μ

    }

    protected abstract void setPatientArrivalTime(Patient patient, double time);
    protected abstract void setPatientDepartureTime(Patient patient, double time);
    protected abstract void setPatientProcessingTime(Patient patient, double time);
    protected abstract Event.EventType getDepartureEventType();
    protected abstract void processPatientDeparture(Event currentEvent);
    protected abstract double getPatientArrivalTime(Patient patient);
    protected abstract Statistics.Stage getStatisticsStage();
}
