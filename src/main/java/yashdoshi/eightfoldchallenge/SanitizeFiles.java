package yashdoshi.eightfoldchallenge;

import java.io.*;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * SanitizeFiles Class is used to Sanitize the Log files, by maintaining separate files for each threadID in a
 * sanitizedLogs folder. Along with sanitization, the maximum concurrent threads for any second is calculated, as all
 * the logs are traversed and stored in a Txt file.
 * Original Log Files : Logs/AssignmentLogDump
 * Sanitized file path : Logs/sanitizedLogs
 * BonusAPI part 1 result : Logs/BonusAPI-Part1.txt
 */
public class SanitizeFiles {
    private static String logFilesPath = "Resources/AssignmentLogDump";
    private static String sanitzedLogFilesPath = "Resources/sanitizedLogs";
    private static int maxConcurrentThreads = 0;
    private static HashSet<String> threads = new HashSet<>();
    private static Timestamp t = null;
    private static Timestamp epoch = null;

    /**
     * The main method begins by creating a Separate folder to maintain Sanitized log files and then calls the
     * method to read them. It also calls the method to return the maximum concurrent threads for any second.
     * @param args No parameters required
     * @throws IOException In case there is an issue while reading, writing and searching file/directory operations.
     * @throws ParseException Parsing the string(Log) in a particular format especially for the Date could create an
     * issue.
     */
    public static void main(String[] args) throws IOException, ParseException, URISyntaxException {
        //Creating a File object

        File file = new File(sanitzedLogFilesPath);
        //Creating the directory
        file.mkdir();
        System.out.println("***********Sanitizing Files***********");
        readLogFiles();
        System.out.println("***********Files are Sanitized***********");
        getMaximumConcurrentThreads();
    }

    /**
     * This method creates a BufferReader array for the number of Log files present in the AssignmentLogDump for the
     * purpose of reading them simultaneously.
     * @throws IOException
     * @throws ParseException
     */
    public static void readLogFiles() throws IOException, ParseException, URISyntaxException {

        int numberOfLogFiles =  new File(logFilesPath).list().length;
        BufferedReader[] fileReader = null;
        try {
            fileReader = new BufferedReader[numberOfLogFiles];
            File folder = new File(logFilesPath);
            File[] listOfFiles = folder.listFiles();
            int i = 0;
            for(File f : listOfFiles) {
                if(!f.isDirectory()) {
                    fileReader[i] = new BufferedReader(new FileReader(f));
                }
                i++;
            }
            mergeKFiles(fileReader);
        } finally {
            //Close all the streams
            for(int j = 0; j < fileReader.length; j++) {
                fileReader[j].close();
            }
        }


    }

    /**
     * This method is used to read all the Log files simultaneously and puts the logs in the Priority Queue based on
     * their timestamp. While the queue != empty, It will poll the log and write it into it's corresponding
     * thread/user-request file. It will then provide the next log from the file it was polled from.
     * @param fileReader Array of BufferedReaders for every Log file
     * @throws IOException
     * @throws ParseException
     */
    private static void mergeKFiles(BufferedReader[] fileReader) throws IOException, ParseException {
        if(fileReader == null || fileReader.length == 0) {
            return;
        }
        //Custom Comparator for Timestamp
        PriorityQueue<LogLine> pq = new PriorityQueue<>((o1, o2) -> {
            Timestamp t1 = o1.getTimestamp();
            Timestamp t2 = o2.getTimestamp();
            if(t1.before(t2)) {
                return -1;
            }
            else if(t1.after(t2)) {
                return 1;
            }
            return 0;
        });
        //Offer the 1st log of all the Log files
        for(int i = 0; i < fileReader.length; i++) {
            String s = fileReader[i].readLine();
            if(s != null) {
                pq.offer(createLog(s, i));
            }
        }

        while(!pq.isEmpty()) {
            LogLine fileLog = pq.poll();

            /**
             * Since logs are picked in sorted order from all files, maxConcurrentThreads and epoch maintain the highest
             * concurrent threads at a given second
             */
            if(t != null && !fileLog.getTimestamp().equals(t)) {

                if(maxConcurrentThreads < threads.size()) {
                    maxConcurrentThreads = threads.size();
                    epoch = t;
                }
                t = fileLog.getTimestamp();
                threads.clear();

            }
            else {
                t = fileLog.getTimestamp();
                threads.add(fileLog.getThreadId());
            }


            String s = fileReader[fileLog.getFileId()].readLine();
            if(s != null) {
                //Since logs Statements contain breaks, it has to be added to the previous log
                while(s.indexOf("::") == -1) {
                    fileLog.logStatement += "\n"+ s;
                    s = fileReader[fileLog.getFileId()].readLine();
                }
                pq.offer(createLog(s, fileLog.getFileId()));
            }
            writeToFile(fileLog);
        }
    }

    /**
     * Write each log to its corresponding thread/user-request file.
     * @param fileLog
     * @throws IOException
     */
    private static void writeToFile(LogLine fileLog) throws IOException {
        String sanitizedFile = "Resources/sanitizedLogs/"+fileLog.getThreadId() + ".log";
        File file = new File(sanitizedFile);
        BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
        //For every log to be on a new line
        if(file.length() != 0) {
            writer.append("\n");
        }
        String flushLog = getLog(fileLog);
        writer.append(flushLog);
        writer.close();

    }

    /**
     * This method is used to parse the Log polled from the Priority Queue. based on the delimiters provided.
     * @param s  A single log provided by the readLine() of BufferedReader.
     * @param fileId Id of the Log file from which the log was polled from the Priority Queue
     * @return It returns a LogLine object
     * @throws ParseException
     */
    private static LogLine createLog(String s, int fileId) throws ParseException {
        int firstSplit = s.indexOf(':');
        int secondSplit = s.indexOf("::");
        int firstSpace = s.indexOf(' ');
        int secondSpace = s.indexOf(' ',s.indexOf(' ')+1);
        int lastSplit = s.indexOf('-', secondSpace+1);
        int pId = Integer.parseInt(s.substring(0,firstSplit));
        String threadId = s.substring(firstSplit+1,secondSplit);
        String threadName = s.substring(secondSplit+2,firstSpace);
        String logDate = s.substring(firstSpace+1, lastSplit-1);
        //Given the logs have the particular Format of timestamp
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss,SSS");
        Date parsedDate = dateFormat.parse(logDate);
        Timestamp timestamp = new java.sql.Timestamp(parsedDate.getTime());

        String logStatement = s.substring(lastSplit+1).trim();
        LogLine logLine = new LogLine(pId,threadId,threadName,timestamp,logStatement,fileId);
        return logLine;
    }

    /**
     * This method is used to parse the LogLine object back to string to write to a file. StringBuffer is used as string
     * concatenation is time consuming.
     * @param fileLog The logLine object which is polled from the Priority Queue
     * @return A string which is to be written to the corresponding Thread/user-request file
     */
    private static String getLog(LogLine fileLog) {
        StringBuffer logLine = new StringBuffer();
        int processId = fileLog.getProcessId();
        String threadId = fileLog.getThreadId();
        String threadName = fileLog.getThreadName();
        Timestamp time = fileLog.getTimestamp();
        String logStatement = fileLog.getLogStatement();
        logLine.append(processId);
        logLine.append(":");
        logLine.append(threadId);
        logLine.append("::");
        logLine.append(threadName + " ");
        logLine.append(time + " ");
        logLine.append("- ");
        logLine.append(logStatement);
        return logLine.toString();
    }

    /**
     * This method is used to write the output for the BonusAPI-Part1 to a file/
     * @throws IOException
     */
    public static void getMaximumConcurrentThreads() throws IOException {
        File f = new File("Resources/BonusAPI-Part1.txt");
        BufferedWriter writer = new BufferedWriter(new FileWriter(f));
        writer.append("Highest count of concurrent threads running in any second \n");
        writer.append(epoch + "  :  " + maxConcurrentThreads);
        writer.close();
    }

}
