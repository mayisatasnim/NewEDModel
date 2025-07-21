import java.util.*;

public abstract class ServiceStation extends Metrics {
    protected int debug = 0;

    //change for multiple bed support in zones
    protected int numBeds; //servers
    protected int busyBeds = 0; //beds in use
     //time in minutes to decide to LWBS
    protected double meanServiceTime;
    protected double serviceStdDev;
    protected WaitingArea queue;
    protected List<Patient> departedPatients;
    protected List<Patient> arrivedPatients;
    protected List<Patient> lwbsPatients;
    protected PriorityQueue<Event> eventList;
    protected Simulator simulator;


    public ServiceStation(Simulator.StationName stationName, double meanServiceTime, double serviceStdDev, int numBeds, Simulator simulator) {
        super(stationName);
        this.stationName = stationName;
        setServiceTime(meanServiceTime, serviceStdDev);

        //for multiple bed support
        this.numBeds = numBeds;
        this.busyBeds = 0;

        this.queue = new WaitingArea(WaitingArea.PrioritizationPolicy.HIGHER_ACUITY_FIRST);
        this.departedPatients = new ArrayList<>();
        this.arrivedPatients = new ArrayList<>();
        this.lwbsPatients = new ArrayList<>();
        this.eventList = simulator.eventList;
        this.simulator = simulator;
    }

    public void setServiceTime(double meanServiceTime, double serviceStdDev) {
        this.meanServiceTime = meanServiceTime;
        this.serviceStdDev = serviceStdDev;
    }

    public void addPatient(Event currentEvent) {
        Patient patient = currentEvent.patient;
        queue.add(patient);
        arrivedPatients.add(patient);
        totalArrivals++;
        updatePatientLocation(patient);
        patient.scheduleDecideToLWBS(simulator);
        setPatientArrivalTime(patient, currentEvent.eventTime);
        setPatientDepartureTime(patient, Double.POSITIVE_INFINITY);

        if (debug == 1) {
            System.out.println("[" + stationName + "]: Added " + patient.id + " to queue @T: " + currentEvent.eventTime);
        }

        // if available bed
        if (busyBeds < numBeds) {
            scheduleNextDeparture(currentEvent.eventTime);
        }

    }

    protected void scheduleNextDeparture(double currentTime) {
        Patient nextPatient = queue.poll();
        setPatientProcessingTime(nextPatient, currentTime);
        double serviceTime = Utils.getNormal(meanServiceTime, serviceStdDev);
        double nextDepartureTime = currentTime + serviceTime;
        eventList.add(new Event(nextDepartureTime, getDepartureEventType(), nextPatient));
        busyBeds++;
        if (debug == 1) {
            System.out.println("[" + stationName + "]: Next departure: " + nextDepartureTime);
        }
    }

    protected void updatePatientLocation(Patient patient) {
        patient.currentStationName = stationName;
    }

    public void departServiceStation(Event currentEvent) {

        if (debug == 1) {
            System.out.println(currentEvent.patient.id + " DP_" + stationName + ": " + currentEvent.eventTime);
        }

        sendToAppropriateNextStation(currentEvent);
        setPatientDepartureTime(currentEvent.patient, currentEvent.eventTime);
        departedPatients.add(currentEvent.patient);
        busyBeds--;

        //start service for another patient if queue isn't empty
        if (!queue.isEmpty()) {
            scheduleNextDeparture(currentEvent.eventTime);
        }


    }

    public void printQuickStats() {
        computeMetrics();
        System.out.println("\n[" + stationName + "]: Quick Stats");
        System.out.println("Total arrivals: " + totalArrivals);
        System.out.println("% Arrivals rel to ED: " + String.format("%.2f", (totalArrivals * 100.0 / simulator.totalArrivals)) + "%");
        if(stationName != Simulator.StationName.SORT) {
            System.out.println("% Arrivals rel to " + getPrecedingStation().stationName + " output: " + String.format("%.2f", (totalArrivals * 100.0 / getPrecedingStation().totalArrivals)) + "%");
        }
        if(stationName == Simulator.StationName.ERU) {
            System.out.println("% Arrivals rel to SORT: " + String.format("%.2f", (totalArrivals * 100.0 / simulator.sortNurse.totalArrivals)) + "%");
        }
        System.out.println("Total processed: " + departedPatients.size());
        System.out.println("Avg arrivals per day: " + (totalArrivals / (double) simulator.numDays));

        System.out.println("% arrivals at this station: " + (totalArrivals/ (double) simulator.totalArrivals)*100.0 + "%");

        System.out.println("Current Queue size[waiting]: " + queue.size());
        System.out.println("[R]Mean " + stationName + " waiting time: " + Utils.formatMinsToHours(realMeanWaitingTime));
        System.out.println("[R]Mean " + stationName + " service time: " + Utils.formatMinsToHours(realMeanServiceTime));
        System.out.println("[E]Mean " + stationName + " service time: " + Utils.formatMinsToHours(meanServiceTime));
        System.out.println("[R]Mean " + stationName + " LOS [ResponseTime]: " + Utils.formatMinsToHours(realResponseTime));
        System.out.println("[R]Mean " + stationName + " Inter-Arrival Time: " + Utils.formatMinsToHours(realMeanInterArrivalTime));
    }

    public void computeMetrics() {
        realMeanWaitingTime = Statistics.calculateMean(departedPatients, stationName, Statistics.Property.WAITING_TIME);
        realMeanServiceTime = Statistics.calculateMean(departedPatients, stationName, Statistics.Property.PROCESSING_TIME);
        realResponseTime = Statistics.calculateMean(departedPatients, stationName, Statistics.Property.RESPONSE_TIME);
        realMeanInterArrivalTime = Statistics.calculateMean(arrivedPatients, stationName, Statistics.Property.INTER_ARRIVAL_TIME);
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
    protected abstract void sendToAppropriateNextStation(Event currentEvent);
    protected abstract double getPatientArrivalTime(Patient patient);
    protected abstract ServiceStation getPrecedingStation();
}
