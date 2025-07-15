import java.util.List;

public class Statistics {
    enum Stage {
        SORTING,
        REGISTRATION,
        TRIAGE,
        ZONE,
        ED
    }
    enum Property {
        WAITING_TIME,
        SERVICE_TIME,
        LOS
    }
    public static double calculateAverage(List<Patient> patients, Stage stage,Property property) {
        double sum = 0.0;
        int count = 0;
        for (Patient p : patients) {
            double value = 0.0;
            switch (stage) {
                case SORTING:
                    switch (property) {
                        case WAITING_TIME:
                            value = p.sortingPT - p.sortingAT;
                            break;
                        case SERVICE_TIME:
                            value = p.sortingDT - p.sortingPT;
                            break;
                        case LOS:
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
                        case SERVICE_TIME:
                            value = p.registrationDT - p.registrationPT;
                            break;
                        case LOS:
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
                        case SERVICE_TIME:
                            value = p.triageDT - p.triagePT;
                            break;
                        case LOS:
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
                        case SERVICE_TIME:
                            value = p.zoneDT - p.zonePT;
                            break;
                        case LOS:
                            value = p.zoneDT - p.zoneAT;
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown property for ZONE stage");
                    }
                    break;
                case ED:
                    switch (property) {
                        case WAITING_TIME:
                            value = p.zonePT - p.sortingAT;
                            break;
                        case SERVICE_TIME:
                            throw new IllegalArgumentException("Service time not applicable for ED stage");
                        case LOS:
                            value = p.zoneDT - p.sortingAT;
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
}
