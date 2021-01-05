package yashdoshi.eightfoldchallenge;

import java.sql.Timestamp;

/**
 * LogLine class maintains a single log from the Log files.
 */
public class LogLine {
    public int processId;
    public String threadId;
    public String threadName;
    public Timestamp timestamp;
    public String logStatement;
    /**
     * fileId is used to maintain the Un-sanitized Log file from which the log was polled
     */
    public int fileId;

    LogLine(int pId, String tId, String tName, Timestamp t1, String statement, int fileId) {
        this.processId = pId;
        this.threadId = tId;
        this.threadName = tName;
        this.timestamp = t1;
        this.logStatement = statement;
        this.fileId = fileId;

    }
    /**
     * Getter for Log timestamp
     * @return timestamp of the log
     */
    public Timestamp getTimestamp() {
        return timestamp;
    }

    /**
     * Getter for fileId of the Log file from where the log was polled
     * @return fileId of the log
     */
    public int getFileId() {
        return fileId;
    }

    /**
     * Getter for the Logged statement
     * @return logStatement of the particular Log
     */
    public String getLogStatement() {
        return logStatement;
    }

    /**
     * Getter for the Process id on which the thread is running
     * @return process Id of the log.
     */
    public int getProcessId() {
        return processId;
    }

    /**
     * Getter for the thread id of the Log
     * @return threadId of the log
     */
    public String getThreadId() {
        return threadId;
    }

    /**
     * Getter for the Thread name of the Log
     * @return threadName of the log
     */
    public String getThreadName() {
        return threadName;
    }

}
