public class Patient {
    static boolean debug = false;

    //identifier information
    public String id;
    public int age;
    public int ESILevel; 
    public String arrivalMode;
    public String acuity;

    //lwbs
    public boolean hasLWBS;
    public double LWBSProbability;
    public double LWBSTime;          

    // Station timing
    public double sortingAT;
    public double sortingPT;
    public double sortingDT;
    public double registrationAT;
    public double registrationPT;
    public double registrationDT;
    public double triageAT;
    public double triagePT;
    public double triageDT;
    public double zoneAT;
    public double zonePT;
    public double zoneDT;

    public Patient(int regNo) {
        // Initialize all fields
        this.id = "P_" + regNo; 
        this.age = inferAge();
        this.arrivalMode = inferArrivalMode();
        this.ESILevel = inferESILevel(this.arrivalMode);
        this.acuity = inferAcuity(this.ESILevel);
        this.hasLWBS = false;
        this.LWBSProbability = 0.0;
        this.LWBSTime = 0.0;
        if(debug) {
            printDebugInfo();
        }
    }


    private void printDebugInfo() {
        System.out.println(this.id + "-arrivalMode: " + arrivalMode);
        System.out.println(this.id + "-ESILevel: " + ESILevel);
        System.out.println(this.id + "-acuity: " + acuity);
        System.out.println(this.id + "-age: " + age);
        System.out.println(this.id + "-LWBS prob: " + LWBSProbability);
    }

    public String inferArrivalMode(){
        String patientType;
        double r = Math.random();
        if (r < 0.82) patientType = "Walk-in";
        else patientType = "Ambulance";

        return patientType;
    }

    public int inferESILevel(String arrivalMode){
        double r = Math.random(); // gets a number between 0 and 1

        if (arrivalMode.equals("Walk-in")) {
            if (r < 0.01271) return 1;
            else if (r < 0.16441) return 2;
            else if (r < 0.54325) return 3;
            else if (r < 0.80483) return 4;
            else return 5;

        } else {
            if (r < 0.00279) return 1;
            else if (r < 0.0369) return 2;
            else if (r < 0.11925) return 3;
            else if (r < 0.17667) return 4;
            else return 5;
        }

    }

    public String inferAcuity(int ESILevel){
        switch(ESILevel){
            case 1: return "High";
            case 2: return "Moderate";
            case 3: return "Moderate";
            case 4: return "Low";
            case 5: return "Low";
            default: return "Unknown";
        }
    }

    public void computeLWBSProbability(
            int hostQueueSize,
            double arrivalRate,
            double doorToProviderTime,
            int currentHour
    ) {
        LWBSModel model = LWBSModel.getInstance();
        this.LWBSProbability = model.predictLWBSProbability(
                this, hostQueueSize, arrivalRate,
                doorToProviderTime, currentHour
        );
    }

    public int inferAge() {
        double r = Math.random();

        // Based on typical ED age distribution
        if (r < 0.20) return 18 + (int)(Math.random() * 27); // 18-44 (20%)
        else if (r < 0.40) return 45 + (int)(Math.random() * 20); // 45-64 (20%)
        else if (r < 0.70) return 65 + (int)(Math.random() * 15); // 65-79 (30%)
        else if (r < 0.90) return 80 + (int)(Math.random() * 10); // 80-89 (20%)
        else return 0 + (int)(Math.random() * 18); // 0-17 (10%)
    }
    public static void main(String[]args){
        // Test both modes
        System.out.println("=== Testing CLASS_LEVEL_DEBUG = true ===");
        Patient.debug = true;
        boolean debugLWBS = true;
        Patient pt = new Patient(1);

        if(debugLWBS) {
            // Simulate different system states
            System.out.println("\n=== LWBS Probability Under Different Conditions ===");
            // Normal conditions
            pt.computeLWBSProbability(20,  15.0, 45.0, 10); // 10 AM, normal load
            System.out.println("Normal conditions (10 AM, low load): " + String.format("%.3f", pt.LWBSProbability));        
            // Busy evening
            pt.computeLWBSProbability(20,  15.0, 45.0, 10); // 10 AM, normal load
            System.out.println("Busy evening (8 PM, high load): " + String.format("%.3f", pt.LWBSProbability));     
            // Very congested
            pt.computeLWBSProbability(60,  18.0, 35.0, 22); // 10 PM, very high load
            System.out.println("Very congested (10 PM, very high load): " + String.format("%.3f", pt.LWBSProbability));
        }
    }
}
