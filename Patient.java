public class Patient {
    static boolean debug = false;

    //identifier information
    public String id;
    public int age;
    public int level; //patient's ESI level
    public String arrivalMode;
    public String acuity;

    //death tracking
    public boolean isDeceased = false;


    //zone and queue position
    public int zonePosition;
    public int queuePosition;

    //lwbs
    public boolean hasLWBS;
    public double LWBSProbability;
    public double LWBSTime;
    // LWBS eligibility tracking
    public boolean abbreviatedTriageCompleted; // Reason for visit + age collected
    public boolean fullRegistrationCompleted;  // Complete registration process
    public boolean calledForTreatment;         // Whether patient has been called
    public int callAttempts;                   // Number of calling attempts
    public double lastCallTime;                // Time of last call attempt

    //time
    // Overall ED timing
    public double edArrivalTime;
    public double edDepartureTime;

    // Station timing
    public double sortingAT;
    public double sortingDT;
    public double registrationAT;
    public double registrationDT;
    public double triageAT;
    public double triageDT;
    public double waitingRoomAT;
    public double waitingRoomDT;
    public double medicalZoneAT;
    public double medicalZoneDT;

    // Queue timing for each stage
    public double sortingQueueAT;
    public double sortingQueueDT;
    public double registrationQueueAT;
    public double registrationQueueDT;
    public double triageQueueAT;
    public double triageQueueDT;
    public double medicalZoneQueueAT;
    public double medicalZoneQueueDT;

    // --- Triage Process Flags ---
    public boolean sortingCompleted;
    public boolean isRegistered;
    public boolean registrationCompleted;
    public boolean triageCompleted;
    public String assignedZone;


    public Patient() {
        if (debug) {
            computeAllAttributesForDebug();
        } else {
            initializeBasicFields();
        }
        // Always initialize timing and tracking fields regardless of mode
        initializeTimingFields();
        initializeTriageProcessFlags();
        initializeLWBSTracking();

        if(debug){
            printDebugInfo();
        }
    }

    private void computeAllAttributesForDebug() {
        this.arrivalMode = setArrivalMode();
        this.age = generateAge();
        this.level = setLevel(arrivalMode);
        this.acuity = setAcuity(level);
    }

    private void initializeBasicFields() {

        // Initialize to default/unknown values
        this.age = generateAge();
        this.arrivalMode = setArrivalMode();
        this.level = setLevel(arrivalMode);
        this.acuity = setAcuity(level);

    }

    private void initializeTimingFields() {
        // Initialize timing fields
        this.edArrivalTime = 0;
        this.edDepartureTime = 0;

        // Station timing
        this.sortingAT = 0;
        this.sortingDT = 0;
        this.registrationAT = 0;
        this.registrationDT = 0;
        this.triageAT = 0;
        this.triageDT = 0;
        this.waitingRoomAT = 0;
        this.waitingRoomDT = 0;
        this.medicalZoneAT = 0;
        this.medicalZoneDT = 0;

        // Queue timing
        this.sortingQueueAT = 0;
        this.sortingQueueDT = 0;
        this.registrationQueueAT = 0;
        this.registrationQueueDT = 0;
        this.triageQueueAT = 0;
        this.triageQueueDT = 0;
        this.medicalZoneQueueAT = 0;
        this.medicalZoneQueueDT = 0;
    }

    private void initializeTriageProcessFlags() {
        // Initialize triage process flags
        this.sortingCompleted = false;
        this.registrationCompleted = false;
        this.triageCompleted = false;
        this.assignedZone = null;

        // Initialize position tracking
        this.zonePosition = -1;
        this.queuePosition = -1;
    }

    private void initializeLWBSTracking() {
        // Initialize LWBS tracking
        this.hasLWBS = false;
        this.LWBSTime = 0;
        this.LWBSProbability = 0.0;
        this.abbreviatedTriageCompleted = false;
        this.fullRegistrationCompleted = false;
        this.calledForTreatment = false;
        this.callAttempts = 0;
        this.lastCallTime = 0;
    }

    private void printDebugInfo() {
        System.out.println("Patient type(arrivalMode) is " + arrivalMode);
        System.out.println("Patient level is " + level);
        System.out.println("Patient age is " + age);
        System.out.println("Patient acuity is " + acuity);
        System.out.println("LWBS Probability is " + LWBSProbability);
    }

    public String setArrivalMode(){
        String patientType;
        double r = Math.random();
        if (r < 0.82) patientType = "Walk-in";
        else patientType = "Ambulance";

        return patientType;
    }

    public int setLevel(String arrivalMode){
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

    public String setAcuity(int level){
        switch(level){
            case 1: return "High";
            case 2: return "Moderate";
            case 3: return "Moderate";
            case 4: return "Low";
            case 5: return "Low";

        }
        return "";
    }


    /**
     * Update LWBS probability based on current system state
     * Called during simulation when system state changes
     */
    public void updateLWBSProbability(
            int waitingRoomCount,
            int boardersCount,
            double arrivalRate,
            double doorToProviderTime,
            int currentHour
    ) {
        LWBSModel model = LWBSModel.getInstance();
        this.LWBSProbability = model.predictLWBSProbability(
                this, waitingRoomCount, boardersCount, arrivalRate,
                doorToProviderTime, currentHour
        );
    }

    /**
     * Complete abbreviated triage (collect reason for visit and age)
     */
    public void completeAbbreviatedTriage() {
        this.abbreviatedTriageCompleted = true;
    }

    public int generateAge() {
        double r = Math.random();

        // Based on typical ED age distribution
        if (r < 0.20) return 18 + (int)(Math.random() * 27); // 18-44 (20%)
        else if (r < 0.40) return 45 + (int)(Math.random() * 20); // 45-64 (20%)
        else if (r < 0.70) return 65 + (int)(Math.random() * 15); // 65-79 (30%)
        else if (r < 0.90) return 80 + (int)(Math.random() * 10); // 80-89 (20%)
        else return 0 + (int)(Math.random() * 18); // 0-17 (10%)
    }


    //=======================================
    // Stage-Specific Attribute Computation Methods
    //=======================================

    public void determineSortingStageAttributes() {
        if (arrivalMode == null) {
            this.arrivalMode = setArrivalMode();
            if (debug) {
                System.out.println("Sorting Stage - Patient arrival mode determined: " + arrivalMode);
            }
        }
    }

    public void determineRegistrationStageAttributes() {
        // in real ED, age and insurance information is collected here
        if (age == -1) {
            this.age = generateAge();
            if (debug) {
                System.out.println("Registration Stage - Patient age determined: " + age);
            }
        }

        // Complete full registration
        this.fullRegistrationCompleted = true;
        this.isRegistered = true;
    }

    public void determineTriageStageAttributes() {
        // Ensure prerequisite information is available
        if (arrivalMode == null) {
            determineSortingStageAttributes();
        }

        if (level == -1) {
            this.level = setLevel(arrivalMode);
            if (debug) {
                System.out.println("Triage Stage - Patient ESI level determined: " + level);
            }
        }

        if (acuity == null) {
            this.acuity = setAcuity(level);
            if (debug) {
                System.out.println("Triage Stage - Patient acuity determined: " + acuity);
            }
        }
    }

    //======================================
    // Timing and Queue Management Methods
    //======================================

    public void setEDArrivalTime(double time) {
        this.edArrivalTime = time;
    }

    public void setEDDepartureTime(double time) {
        this.edDepartureTime = time;
    }

    public void enterSortingQueue(double time) {
        this.sortingQueueAT = time;
    }

    public void exitSortingQueueEnterSorting(double time) {
        this.sortingQueueDT = time;
        this.sortingAT = time;
    }

    public void exitSorting(double time) {
        this.sortingDT = time;
    }

    public void enterRegistrationQueue(double time) {
        this.registrationQueueAT = time;
    }

    public void exitRegistrationQueueEnterRegistration(double time) {
        this.registrationQueueDT = time;
        this.registrationAT = time;
    }

    public void exitRegistration(double time) {
        this.registrationDT = time;
    }

    public void enterTriageQueue(double time) {
        this.triageQueueAT = time;
    }

    public void exitTriageQueueEnterTriage(double time) {
        this.triageQueueDT = time;
        this.triageAT = time;
    }

    public void exitTriage(double time) {
        this.triageDT = time;
    }

    public void enterWaitingRoom(double time) {
        this.waitingRoomAT = time;
    }

    public void exitWaitingRoom(double time) {
        this.waitingRoomDT = time;
    }

    public void enterMedicalZoneQueue(double time) {
        this.medicalZoneQueueAT = time;
    }

    public void exitMedicalZoneQueueEnterMedicalZone(double time) {
        this.medicalZoneQueueDT = time;
        this.medicalZoneAT = time;
    }

    public void exitMedicalZone(double time) {
        this.medicalZoneDT = time;
        this.setEDDepartureTime(time);
    }


    public void setStageArrivalTime(String stageName, double time) {
        switch(stageName) {
            case "Sort":
                this.sortingAT = time;
                break;
            case "Register":
                this.registrationAT = time;
                break;
            case "Triage":
                this.triageAT = time;
                break;
            case "Wait":
                this.waitingRoomAT = time;
                break;
            case "MedicalZone":
                this.medicalZoneAT = time;
                break;
        }
    }

    public void setStageDepartureTime(String stageName, double time) {
        switch(stageName) {
            case "Sort":
                this.sortingDT = time;
                break;
            case "Register":
                this.registrationDT = time;
                break;
            case "Triage":
                this.triageDT = time;
                break;
            case "Wait":
                this.waitingRoomDT = time;
                break;
            case "MedicalZone":
                this.medicalZoneDT = time;
                break;
        }
    }

    public boolean checkDeathRisk(double currentTime, double treatmentStartTime) {
        if (level > 2) return false;

        double duration = currentTime - treatmentStartTime; // in minutes
        double baseHazard = (level == 1) ? 0.07 : 0.015;
        double ageFactor = (age >= 80) ? 2.0 : (age >= 65 ? 1.5 : 1.0);
        double riskRate = baseHazard * ageFactor;
        double deathProb = 1 - Math.exp(-riskRate * (duration / 60.0)); // convert to hours
        return Math.random() < deathProb;
    }


    public static void main(String[]args){
        // Test both modes
        System.out.println("=== Testing CLASS_LEVEL_DEBUG = true ===");
        Patient.debug = true;
        Patient pt = new Patient();

        // Demonstrate improved LWBS prediction with dynamic system state
        System.out.println("=== Patient Information ===");
        System.out.println("Age: " + pt.age);
        System.out.println("Acuity: " + pt.acuity);
        System.out.println("Arrival Mode: " + pt.arrivalMode);
        System.out.println("ESI Level: " + pt.level);

        // Simulate different system states
        System.out.println("\n=== LWBS Probability Under Different Conditions ===");

        // Normal conditions
        pt.updateLWBSProbability(20, 5, 15.0, 45.0, 10); // 10 AM, normal load
        System.out.println("Normal conditions (10 AM, low load): " + String.format("%.3f", pt.LWBSProbability));

        // Busy evening
        pt.updateLWBSProbability(45, 12, 28.0, 90.0, 20); // 8 PM, high load
        System.out.println("Busy evening (8 PM, high load): " + String.format("%.3f", pt.LWBSProbability));

        // Very congested
        pt.updateLWBSProbability(60, 18, 35.0, 150.0, 22); // 10 PM, very high load
        System.out.println("Very congested (10 PM, very high load): " + String.format("%.3f", pt.LWBSProbability));
    }
}
