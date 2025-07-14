public class QueueManager {
    public enum Stage {
        Sort, Register, Triage, Wait, MedicalZone
    }

    private Patient[] queue;
    private int maxSize;
    private int size;
    private Stage stage;

    public QueueManager(int capacity, Stage stage) {
        this.maxSize = capacity;
        this.queue = new Patient[maxSize];
        this.size = 0;
        this.stage = stage;
    }

    public void add(Patient p, double currentTime) {
        if (size >= maxSize) {
            System.out.println("Queue is full at stage: " + stage);
            return;
        }

        queue[size] = p;
        p.queuePosition = size + 1;

        switch (stage) {
            case Sort: p.enterSortingQueue(currentTime); break;
            case Register: p.enterRegistrationQueue(currentTime); break;
            case Triage: p.enterTriageQueue(currentTime); break;
            case Wait: p.enterWaitingRoom(currentTime); break;
            case MedicalZone: p.enterMedicalZoneQueue(currentTime); break;
        }

        size++;
    }

    public void addWithPriority(Patient p, double currentTime) {
        if (size >= maxSize) {
            System.out.println("Queue is full (priority insert)");
            return;
        }

        int insertIndex = size;
        for (int i = 0; i < size; i++) {
            if (p.level < queue[i].level) {
                insertIndex = i;
                break;
            }
        }

        for (int j = size; j > insertIndex; j--) {
            queue[j] = queue[j - 1];
            if (queue[j] != null) queue[j].queuePosition = j + 1;
        }

        queue[insertIndex] = p;
        p.queuePosition = insertIndex + 1;
        p.enterWaitingRoom(currentTime);

        size++;
    }

    public Patient pop() {
        if (size == 0) return null;

        Patient next = queue[0];

        for (int i = 0; i < size - 1; i++) {
            queue[i] = queue[i + 1];
            if (queue[i] != null) queue[i].queuePosition = i + 1;
        }

        queue[size - 1] = null;
        size--;

        next.queuePosition = -1;
        return next;
    }

    public boolean remove(Patient p) {
        for (int i = 0; i < size; i++) {
            if (queue[i] == p) {
                for (int j = i; j < size - 1; j++) {
                    queue[j] = queue[j + 1];
                    if (queue[j] != null) queue[j].queuePosition = j + 1;
                }
                queue[size - 1] = null;
                size--;
                p.queuePosition = -1;
                return true;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int getSize() {
        return size;
    }


    public int checkForAbandonment(double currentTime, double maxWaitTime) {
        int abandonedCount = 0;

        for (int i = 0; i < size; i++) {
            Patient p = queue[i];

            //how long has patient been in the queue
            double waitTime = switch (stage) {
                case Sort -> currentTime - p.sortingQueueAT;
                case Register -> currentTime - p.registrationQueueAT;
                case Triage -> currentTime - p.triageQueueAT;
                case Wait -> currentTime - p.waitingRoomAT;
                case MedicalZone -> currentTime - p.medicalZoneQueueAT;
            };

            double probability = p.LWBSProbability;
            double threshold = 1 - Math.exp(-waitTime * probability);

            if (waitTime >= maxWaitTime || Math.random() < threshold) {
                System.out.printf("[Queue] Patient %s abandoned queue (wait: %.2f, prob: %.2f)\n", p.id, waitTime, threshold);
                remove(p);
                i--;
                abandonedCount++;
            }
        }

        return abandonedCount;
    }

}

