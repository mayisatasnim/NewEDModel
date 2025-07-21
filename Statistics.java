import java.util.List;
public abstract class Statistics{
    enum Property {
        WAITING_TIME,
        PROCESSING_TIME,
        RESPONSE_TIME,
        DOOR_TO_PROVIDER_TIME,
        INTER_ARRIVAL_TIME     
    }    
    
    // Overloaded method for TreeSet - much more efficient!
    public static double calculateMean(Simulator simulator, Simulator.StationName stationName, Property property) {
        if(stationName == Simulator.StationName.ED) {
           if(property == Property.DOOR_TO_PROVIDER_TIME){
                double sum = 0.0;
                for (Patient p : simulator.edDisposedPatients) {
                    sum += p.getDoorToProviderTime();
                }
                return sum / (double) simulator.edDisposedPatients.size();
           }

           if(property == Property.RESPONSE_TIME){
                double sum = 0.0;
                for (Patient p : simulator.edDisposedPatients) {
                    sum += p.getEDResponseTime();
                }
                return sum / (double) simulator.edDisposedPatients.size();
           }
           throw new IllegalArgumentException("[STATISTICS-ERROR]ED station does not have a mean for this property.");
        }

        ServiceStation station = simulator.getStationByName(stationName);
        if (property == Property.INTER_ARRIVAL_TIME) {
            if (station.totalArrivals < 2) return 0.0;
            return station.totalInterArrivalTime() / (double)(station.totalArrivals - 1);
        }
        
        if(property == Property.WAITING_TIME) {
            double sum = 0.0;
            for (Patient p : station.departedPatients) {
                sum += station.getWaitingTime(p);
            }
            return sum / (double)station.departedPatients.size();
        }

        if(property == Property.PROCESSING_TIME) {
            double sum = 0.0;
            for (Patient p : station.departedPatients) {
                sum += station.getServiceTime(p);
            }
            return sum / (double)station.departedPatients.size();
        }

        if(property == Property.RESPONSE_TIME) {
            double sum = 0.0;
            for (Patient p : station.departedPatients) {
                sum += station.getResponseTime(p);
            }
            return sum / (double)station.departedPatients.size();
        }

        throw new IllegalArgumentException("[STATISTICS-ERROR]Invalid property: " + property + " for station: " + stationName);
    }

    public static int countDeaths(List<Patient> patients) {
        int deaths = 0;
        for (Patient p : patients) {
            if (p.died) {
                deaths++;
            }
        }
        return deaths;
    }

    public static int countLWBS(List<Patient> patients) {
        int count = 0;
        for (Patient p : patients) {
            if (p.hasLWBS) count++;
        }
        return count;
    }

}

