import java.util.PriorityQueue;

public class Simulator {
    int debug = 1;
    double arrivalRate = 10 / 60.0;
    double currentTime = 0;
    double dayEnds = 24 * 60;
    double nextArrivalTime = 0;
    int totalArrivals = 0;

    SortNurse sortNurse;
    Registration registration;
    //event calendar
    PriorityQueue<Event> eventList;



    public Simulator(){
        eventList = new PriorityQueue<>();
        registration = new Registration();
        sortNurse = new SortNurse(50,registration,eventList);

        //schedule first arrival
        scheduleNextEDArrival();
    }

    public void begin(){

        while (currentTime < dayEnds) {
            if (!eventList.isEmpty()){
                Event currentEvent = eventList.poll();
                currentTime = currentEvent.eventTime;
                totalArrivals++;
                switch (currentEvent.type){
                    case edArrival:
                        sortNurse.addPatient(currentEvent);
                        scheduleNextEDArrival();
                        break;
                    case sortDeparture:
 //                       System.out.println("patient departed");
                        sortNurse.departSortingNurse();

                    default:
                        System.out.println("unknown event");
                }
                if (debug == 1) {
                    System.out.println("Total arrivals: " + totalArrivals);
                }

            }


        }
    }

    public void scheduleNextEDArrival(){
        double interEDArrivalTime = Utils.getExp(10/60.0);
        double nextEDArrivalTime = currentTime + interEDArrivalTime;
        Patient newPatient = new Patient();
        newPatient.sortingAT = nextEDArrivalTime;
        eventList.add(new Event(nextEDArrivalTime,Event.EventType.edArrival, newPatient));

        if (debug == 1) {
            System.out.println("Patient arrived at: " + nextEDArrivalTime);
            System.out.println("Level: " + newPatient.level + " Type: " + newPatient.arrivalMode);
        }


    }
    public static void main(String[]args){
        Simulator sim = new Simulator();
        sim.begin();
    }
}
