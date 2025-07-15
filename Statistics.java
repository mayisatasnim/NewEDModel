import java.util.List;

public abstract class Statistics extends Metrics{
    enum Stage {
        SORTING,
        REGISTRATION,
        TRIAGE,
        ZONE,
        ED
    }
    enum Property {
        WAITING_TIME,
        PROCESSING_TIME,
        RESPONSE_TIME,
        DOOR_TO_PROVIDER_TIME,
        INTER_ARRIVAL_TIME     
    }
    public Statistics(String stationName) {
        super(stationName);
    }
    public static double calculateMean(List<Patient> patients, Stage stage, Property property) {
        double sum = 0.0;
        int count = 0;
        for (Patient p : patients) {
            double value = 0.0;
            if(property == Property.INTER_ARRIVAL_TIME) {
                return totalInterArrivalTime(patients, stage) / patients.size();
            }
            switch (stage) {
                case SORTING:
                    switch (property) {
                        case WAITING_TIME:
                            value = p.sortingPT - p.sortingAT;
                            break;
                        case PROCESSING_TIME:
                            value = p.sortingDT - p.sortingPT;
                            break;
                        case RESPONSE_TIME:
                            value = p.sortingDT - p.sortingAT;
                            break;        
                        default:
                            throw new IllegalArgumentException("Unknown property for SORTING stage");
                    }
                    break;
                case REGISTRATION:
                    switch (property) {
                        case WAITING_TIME:
                            value = p.registrationPT - p.registrationAT;
                            break;
                        case PROCESSING_TIME:
                            value = p.registrationDT - p.registrationPT;
                            break;
                        case RESPONSE_TIME:
                            value = p.registrationDT - p.registrationAT;
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown property for REGISTRATION stage");
                    }
                    break;
                case TRIAGE:
                    switch (property) {
                        case WAITING_TIME:
                            value = p.triagePT - p.triageAT;
                            break;
                        case PROCESSING_TIME:
                            value = p.triageDT - p.triagePT;
                            break;
                        case RESPONSE_TIME:
                            value = p.triageDT - p.triageAT;
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown property for TRIAGE stage");
                    }
                    break;
                case ZONE:
                    switch (property) {
                        case WAITING_TIME:
                            value = p.zonePT - p.zoneAT;
                            break;
                        case PROCESSING_TIME:
                            value = p.zoneDT - p.zonePT;
                            break;
                        case RESPONSE_TIME:
                            value = p.zoneDT - p.zoneAT;
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown property for ZONE stage");
                    }
                    break;
                case ED:
                    switch (property) {
                        case RESPONSE_TIME:
                            value = p.zoneDT - p.sortingAT;
                            break;
                        case DOOR_TO_PROVIDER_TIME:
                            value = p.zonePT - p.sortingAT;
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown property for ED stage");
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unknown stage");
            }
            sum += value;
            count++;
        }
        return count > 0 ? sum / count : 0.0;
    }
    public static double totalInterArrivalTime(List<Patient> patients, Stage stage) {
        double sum = 0.0;
        int count = 0;
        switch (stage) {
            case ED:
            case SORTING:
                for (int i = 1; i < patients.size(); i++) {
                    double interArrivalTime = patients.get(i).sortingAT - patients.get(i - 1).sortingAT;
                    sum += interArrivalTime;
                    count++;
                }
                break;
            case REGISTRATION:
                for (int i = 1; i < patients.size(); i++) {
                    double interArrivalTime = patients.get(i).registrationAT - patients.get(i - 1).registrationAT;
                    sum += interArrivalTime;
                    count++;
                }
                break;
            case TRIAGE:
                for (int i = 1; i < patients.size(); i++) {
                    double interArrivalTime = patients.get(i).triageAT - patients.get(i - 1).triageAT;
                    sum += interArrivalTime;
                    count++;
                }
                break;
            case ZONE:
                for (int i = 1; i < patients.size(); i++) {
                    double interArrivalTime = patients.get(i).zoneAT - patients.get(i - 1).zoneAT;
                    sum += interArrivalTime;
                    count++;
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown stage for inter-arrival time calculation");
        }
        return count > 0 ? sum / count : 0.0;
    }
}
