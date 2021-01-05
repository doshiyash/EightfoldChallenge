package yashdoshi.eightfoldchallenge;
import java.io.*;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * BasicAPI Class is used to iterate over all the Sanitized file one by one (for each thread/user-request) to insert
 * into the HashMap<ThreadId,HashMap<ProcessID, List<Interval>>>. This map will be limited to number of threads/
 * user-requests which is given to be at the most 1000.
 * Given the time range (t1,t2), all the entries of the map are iterated to check if a particular thread/user-request
 * exists in that range.
 * Along with that , the Average and Standard deviation of lifetime of all the threads are calculated and written in a
 * output file.
 * Sanitized file path : Logs/sanitizedLogs
 * BonusAPI part 2 result : Logs/BonusAPI-Part2.txt
 */
public class BasicAPI {

    private static HashMap<String, HashMap<Integer, List<Interval>>> map;
    private static double averageLifetime = 0.0;
    private static long lifetimeSeconds = 0l;
    private static int count = 0;
    private static double stdDev = 0.0;
    private static List<Double> lifeTimes = new ArrayList<>();

    /**
     * The main method is used accept input from the user for the range of timestamp and call the methods to get the
     * Active threads and also to calculate the Average and Standard deviation of the lifetime of all threads.
     * @param args No input
     * @throws IOException In case there is an issue while reading, writing and searching file/directory operations.
     * @throws ParseException Parsing the string(Log) in a particular format especially for the Date could create an
     * issue.
     */
    public static void main(String[] args) throws IOException, ParseException {
        String sanitizedLogFilesPath = "Resources/sanitizedLogs";
        map = new HashMap<>();
        readSanitizedLogFiles(sanitizedLogFilesPath);

        Scanner sc = new Scanner(System.in);
        System.out.println("Please enter startTime(t1) and endTime(t2) on separate lines");
        String t1 = sc.nextLine();
        String t2 = sc.nextLine();
        getActiveThreads(t1,t2, sanitizedLogFilesPath);
        File f = new File("Resources/BonusAPI-Part2.txt");
        BufferedWriter writer = new BufferedWriter(new FileWriter(f));
        getAverageLifeTime(writer);
        getStandardDeviation(writer);
        writer.close();
    }

    /**
     * This method is used to iterate over all the files in the sanitized folder and read each one by one.
     * @param folderPath contains the list of all the sanitized files
     * @throws IOException
     * @throws ParseException
     */
    private static void readSanitizedLogFiles(String folderPath) throws IOException, ParseException {

        File folder = new File(folderPath);
        File[] listOfFiles = folder.listFiles();
        for(File f : listOfFiles) {
            readSingleFile(f);
        }

    }

    /**
     * This method is used to read a single sanitized file log by log.
     * @param f
     * @throws IOException
     * @throws ParseException
     */
    private static void readSingleFile(File f) throws IOException, ParseException {
        BufferedReader reader = new BufferedReader(new FileReader(f));

        String s = reader.readLine();
        while(s != null) {
            while(s.indexOf("::") == -1) {
                s = reader.readLine();
            }
            storeLogInterval(s);
            s = reader.readLine();
        }
        reader.close();
    }

    /**
     * This method parses the Log to get the threadId, processId and Timestamp. If the logStatement contains the
     * Start delimiter, then it's start Interval is added to the map for that process Id.
     * If the logStatement contains  the End delimter, then it's end Interval is added to the map for that processId.
     * @param s
     * @throws ParseException
     */
    private static void storeLogInterval(String s) throws ParseException {
        int firstSplit = s.indexOf(':');
        int secondSplit = s.indexOf("::");
        int firstSpace = s.indexOf(' ');
        int secondSpace = s.indexOf(' ',s.indexOf(' ')+1);
        int lastSplit = s.indexOf('-', secondSpace+1);
        int pId = Integer.parseInt(s.substring(0,firstSplit));
        String threadId = s.substring(firstSplit+1,secondSplit);
        String logDate = s.substring(firstSpace+1, lastSplit-1);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
        Date parsedDate = dateFormat.parse(logDate);
        Timestamp timestamp = new java.sql.Timestamp(parsedDate.getTime());

        String logStatement = s.substring(lastSplit+1).trim();
        if(logStatement.equals("**START**")) {
            //ThreadId doesn't exist in the map.
            if(!map.containsKey(threadId)) {
                map.put(threadId, new HashMap<>());
            }
            //Get the HashMap<ProcessId, List<Intervals>>
            HashMap<Integer, List<Interval>> processes = map.get(threadId);
            if(!processes.containsKey(pId)) {
                processes.put(pId, new ArrayList<>());
            }
            //Get the list and update the startTime in the interval and endTime to be null.
            List<Interval> interval = processes.get(pId);
            Interval previous = new Interval(timestamp,null);
            interval.add(previous);

        }
        else if(logStatement.equals("**END**")) {
            //End cannot be encountered before the Start of the Thread, so no need to check if it exists in the map
            HashMap<Integer, List<Interval>> processes = map.get(threadId);
            List<Interval> intervals = processes.get(pId);
            //A thread cannot start on the same process, unless it is stopped. So update the last interval only.
            Interval interval = intervals.get(intervals.size()-1);
            interval.setEndTime(timestamp);

        }
    }

    /**
     * This method is used to provide the Active threads in this given time range. Iterate over the entrySet of the map,
     * Check if the interval lies in between the range, add the processId in a set.
     * @param t1 start timestamp provided by the user
     * @param t2 end timestamp provided by the user
     * @param path reference to the file the user-request/thread is stored in.
     * @throws ParseException
     * @throws IOException
     */
    private static void getActiveThreads(String t1, String t2, String path) throws ParseException, IOException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss,SSS");
        Date date1 = dateFormat.parse(t1);
        Timestamp startTime = new java.sql.Timestamp(date1.getTime());
        Date date2 = dateFormat.parse(t2);
        Timestamp endTime = new java.sql.Timestamp(date2.getTime());
        if(startTime.after(endTime)) {
            Timestamp t = startTime;
            startTime = endTime;
            endTime = t;
        }
        int numberOfActiveThreads = 0;
        File f = new File("Resources/BasicAPI.log");
        BufferedWriter writer = new BufferedWriter(new FileWriter(f));

        for(Map.Entry threads : map.entrySet()) {

            String threadId = (String) threads.getKey();
            HashMap<Integer, List<Interval>> processes = (HashMap<Integer, List<Interval>>) threads.getValue();
            HashSet<Integer> activeProcesses = new HashSet<>();

            for(Map.Entry process : processes.entrySet()) {
                int processId = (int) process.getKey();
                List<Interval> intervals = (List<Interval>) process.getValue();
                for(int i = 0; i < intervals.size(); i++) {
                    Timestamp start = intervals.get(i).getStartTime();
                    Timestamp end = intervals.get(i).getEndTime();
                    /**
                     * If end == null , the thread has started but doesn't have an end time, so ignore these intervals
                     * for calculating the Avg and timestamp of their lifetimes.
                     */

                    if(end != null) {
                        //Calculate the Average and Standard deviation
                        count++;
                        double seconds = ((end.getTime() - start.getTime())/1000);
                        lifetimeSeconds += seconds;
                        lifeTimes.add(seconds);
                    }

                    if(checkTimeRange(start,end,startTime,endTime)) {
                        activeProcesses.add(processId);
                    }

                }

            }
            //If set is not empty, that thread is active in this time range for a set of processes.
            if(!activeProcesses.isEmpty()) {
                numberOfActiveThreads++;
                writer.append(threadId + ":" + activeProcesses + " - " + path+"/"+threadId+".log");
                writer.append("\n");
            }
        }
        //Store the total threads active in the time range at the end of this file
        String activeThreadsText = "Number of Active threads in the time range ";
        writer.append(activeThreadsText + startTime +" - "+ endTime +" : " + numberOfActiveThreads);
        writer.close();
    }

    /**
     * This method is used to check if the interval start time lies between start and end time or if the interval start
     * time lies before the start time and the interval end time lies after the start time.
     * These conditions guarantee that this thread with interval is active.
     * @param start interval start time
     * @param end  interval end time
     * @param startTime start time provided by the user
     * @param endTime end time provided by the user
     * @return tru or false based on the condition
     */
    public static boolean checkTimeRange(Timestamp start, Timestamp end, Timestamp startTime, Timestamp endTime) {

        if((start.after(startTime) && start.before(endTime))
                || (start.before(startTime) && (end == null || end.after(startTime)))) {
            return true;
        }
        return false;
    }

    /**
     * This method calculates the average of the life time of all the threads
     * @param writer BufferedWriter for writing the average to an output file
     * @throws IOException
     */
    private static void getAverageLifeTime(BufferedWriter writer) throws IOException {
        averageLifetime = lifetimeSeconds/count;
        writer.append("Average of all the threads lifetime for the whole log dump : " + averageLifetime + " s\n");
    }

    /**
     * This method calculates the standard deviation of all the threads
     * @param writer BufferedWriter for writing the std dev to an output file
     * @throws IOException
     */
    private static void getStandardDeviation(BufferedWriter writer) throws IOException {
        double sqMeanDistance = 0;
        for(double lifetime : lifeTimes) {
            sqMeanDistance += Math.pow(lifetime-averageLifetime,2);
        }
        stdDev = Math.sqrt(sqMeanDistance/count);
        writer.append("Standard Deviation of all the threads lifetime for the whole log dump : " + stdDev + " s");
    }

}
