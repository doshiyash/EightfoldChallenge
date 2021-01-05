package yashdoshi.eightfoldchallenge;

import java.sql.Timestamp;

/**
 * Interval class maintains the start and end time of a thread each time it is spawned in any of the processes
 */
public class Interval {

    private Timestamp startTime;
    private Timestamp endTime;

    Interval( Timestamp start, Timestamp end) {
        this.startTime = start;
        this.endTime = end;
    }

    /**
     * Getter for Start time
     * @return start time of the thread
     */
    public Timestamp getStartTime() {
        return startTime;
    }

    /**
     * Getter for End time
     * @return end time of the thread
     */
    public Timestamp getEndTime() {
        return endTime;
    }

    /**
     * Setter for Start time of the Interval
     * @param startTime
     */
    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    /**
     * Setter for End time of the interval
     * @param endTime
     */
    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }
}
