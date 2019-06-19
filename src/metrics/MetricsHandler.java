package metrics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

class MetricsHandler implements HttpHandler {

   // Definitions of CONSTANT literal values:
   private final Integer TOP_WORDS = 5;
   private final String _CALLTYPE = "CALL";
   private final String _MESSAGETYPE = "MSG";
   private final String _DELIVEREDSTATUS = "DELIVERED";
   private final String _SEENSTATUS = "SEEN";
   private final String _OKSTATUS = "OK";
   private final String _NOTOKSTATUS = "KO";
   private final String _KPIFILE = "resources/KPIcounters.dat";

   public void handle(HttpExchange t) throws IOException {

      String response = new String();
      try {
         response = processMetrics(t.getRequestURI().getQuery());
      } catch (ParseException e) {
         e.printStackTrace();
      }
      t.sendResponseHeaders(200, response.length());
      OutputStream os = t.getResponseBody();
      os.write(response.getBytes());
      os.close();
   }


   private String processMetrics(String queryStringParameter) throws IOException, ParseException {

      // MAIN METHOD for the DAY METRICS PROCESSING
      // (queryStringParameter in format YYYY-MM-DD or YYYYMMDD):
      String dateToProcess = queryStringParameter.replace("-", "");
      String filename = "resources/MCP_" +dateToProcess +".json";
      String fileToProcess = "";
      Integer iRowsInTotal = 0;
      Integer iRowsWithFormatErrors = 0;
      ClassLoader classLoader = ClassLoader.getSystemClassLoader();
      File file = new File(classLoader.getResource(filename).getFile());

      // Starting the timer to measure file processing time:
      Instant start = Instant.now();

      // Checking if the date filename exists:
      if (!file.exists() || file.isDirectory()) { 
         System.err.println("The file related to date (" +queryStringParameter +") does NO exist");
         return ("The file related to date (" +queryStringParameter +") does NO exist");
      } else {
         fileToProcess = classLoader.getResource(filename).getPath();
      }

      // Parsing the file, building an array with JSONobjects -one per line- and detecting format errors:
      JSONParser parser = new JSONParser();
      JSONObject jsonObject = new JSONObject();
      ArrayList<JSONObject> jsonObjectsList = new ArrayList<>();
      BufferedReader reader = new BufferedReader(new FileReader(fileToProcess));
      String line;
      while ((line = reader.readLine()) != null) {
         try {
            jsonObject = (JSONObject) parser.parse(line);
            jsonObjectsList.add(jsonObject);
         } catch (Exception e) {
            iRowsWithFormatErrors++;
         }
         iRowsInTotal++;
      }
      reader.close();

      // Creation of the object DayMetrics which will embrace every metric:
      DayMetrics dayMetrics = new DayMetrics(dateToProcess);

      // Initial values already calculated:
      dayMetrics.SetTotalNumOfRows(iRowsInTotal);
      dayMetrics.SetRowsWithFieldsErrors(iRowsWithFormatErrors);

      // Next step in the validation, where invalid rows will get removed from JsonObjectsList:
      jsonObjectsList = validateFields(jsonObjectsList, dayMetrics);

      // Continuing with Basic Metrics like Num of messages and ok/ko calls:
      calculateBasicMetrics(jsonObjectsList, dayMetrics);

      // Metrics based on Country Codes, where every call will get removed from JsonObjectsList, since only MESSAGES metrics remain:
      jsonObjectsList = calculateCCgroupedMetrics(jsonObjectsList, dayMetrics);

      // Last metric to be calculated is Top Ranked words from messages content:
      performMessagesWordRanking(jsonObjectsList, dayMetrics);

      // We stop the measure timer at this moment -In Milliseconds-:
      Instant finish = Instant.now();
      long elapsedTimeInMillis = Duration.between(start, finish).toMillis(); 

      // Last step in dayfile process is submit its KPI counters:
      processKPIs(dayMetrics, elapsedTimeInMillis);

      // The final output report is built:
      return buildResults(dayMetrics);
   }


   private void processKPIs(DayMetrics processedDay, long processingTimeInMillis) {

      // MAIN METHOD for the KPI processing, where KPI counters are calculated and set into KPI file:
      KPIcounters kpi = null;
      ClassLoader classLoader = ClassLoader.getSystemClassLoader();
      File kpifile = new File(classLoader.getResource(_KPIFILE).getFile());

      // Checking if the date filename exists:
      if (!kpifile.exists() || kpifile.isDirectory()) { 
         System.err.println("KPIs file does not exist");
      }

      // Let's read KPI file in order to retrieve KPIcounters instance:
      try {
         FileInputStream fi = new FileInputStream(kpifile);
         ObjectInputStream oi = new ObjectInputStream(fi);

         // Read objects
         kpi = (KPIcounters) oi.readObject();

         // Closure of Input streams:
         oi.close();
         fi.close();

      } catch (FileNotFoundException e) {
         System.out.println("File not found");
      } catch (IOException e) {
         System.out.println("KPIcounters file is empty, creating instance");
         kpi = new KPIcounters();
      } catch (ClassNotFoundException e) {
         System.out.println("KPIcounters file is empty, creating instance");
         kpi = new KPIcounters();
         e.printStackTrace();
      }

      // Calculating and adding values to KPIcounters -Only if the processed date does not exist yet-:
      if (!kpi.ContainsDate(processedDay.GetId())) {
         // then we add the processed date:
         kpi.AddProcessedDate(processedDay.GetId());

         // and the remaining counters:
         kpi.AddTotalRows(processedDay.GetTotalNumOfRows());
         kpi.AddTotalCalls(processedDay.GetTotalNumOfCalls());
         kpi.AddTotalMessages(processedDay.GetTotalNumOfMessages());
         kpi.AddOriginCC(processedDay.GetOriginCCsList());
         kpi.AddDestinationCC(processedDay.GetDestinationCCsList());
         kpi.AddProcessTime(processingTimeInMillis);
      }

      // And write the object to the KPI file, in order to make it persistence between service executions:
      try {
         FileOutputStream f = new FileOutputStream(kpifile);
         ObjectOutputStream o = new ObjectOutputStream(f);

         // Write objects to file
         o.writeObject(kpi);

         // Close Output streams:
         o.close();
         f.close();
                
      } catch (FileNotFoundException e) {
         System.out.println("File not found");
      } catch (IOException e) {
         e.printStackTrace();
      }
   }


   public String buildResults(DayMetrics processedDay) {

      // This method builds and formats the results into a readable output:
      String Results = ("METRICS for day " +processedDay.GetId() +":\n-------------------------");

      Results+=("\n   Number of rows with missing fields: " +processedDay.GetRowsWithMissingFields());
      Results+=("\n   Number of rows with fields errors: " +processedDay.GetRowsWithFieldsErrors());
      Results+=("\n   Number of MESSAGES with blank content: " +processedDay.GetMessagesWithBlankContent());
      Results+=("\n   Percentage of Successful (OK) CALLs: " +processedDay.GetOKcallsPercentage() +"%");
      Results+=("\n   Calls details grouped by Country Code: " +processedDay.GetCallsGroupedByCC());
      Results+=("\n   Ranking of word frequency in MESSAGES: " +processedDay.GetWordRanking());       

      return Results;
   }


   public ArrayList<JSONObject> validateFields(ArrayList<JSONObject> list, DayMetrics processedDay) {

      // This method checks missing fields and valid values
      ArrayList<JSONObject> cleanList = new ArrayList<>();
      Iterator<JSONObject> it = list.iterator();
      Integer iRowsWithMissingFields = 0;
      Integer iRowsWithFieldsErrors = 0;

      while(it.hasNext()){
         JSONObject item=it.next();
         // Firstly we look for missing mandatory fields:
         if (item.get("message_type") == null || item.get("timestamp") == null || item.get("origin") == null    || item.get("destination") == null) {
            iRowsWithMissingFields++;
         // Then we look for missing fields based on the message_type
         } else if (item.get("message_type").equals(_CALLTYPE) && (item.get("duration") == null || item.get("status_code") == null || item.get("status_description") == null)) {
            iRowsWithMissingFields++;
         } else if (item.get("message_type").equals(_MESSAGETYPE) && (item.get("message_content") == null || item.get("message_status") == null)) {
            iRowsWithMissingFields++;
         } else {
            // Secondly -if no mandatory fields are missing- we validate fields' values: 
            if (item.get("message_type").equals(_CALLTYPE) && ((!item.get("status_code").equals(_OKSTATUS) && !item.get("status_code").equals(_NOTOKSTATUS)) || item.get("duration").equals(""))) {
               iRowsWithFieldsErrors++;
            } else if (item.get("message_type").equals(_MESSAGETYPE) && (item.get("message_status").equals(_DELIVEREDSTATUS) && item.get("message_status").equals(_SEENSTATUS))) {
               iRowsWithFieldsErrors++;
            } else if (!item.get("message_type").equals(_CALLTYPE) && !item.get("message_type").equals(_MESSAGETYPE)) {
               iRowsWithFieldsErrors++;
            } else {
               // Valid register is inserted in cleanList
               cleanList.add(item);
            }
         }
      }

      // Setting the results of the validation:
      processedDay.SetRowsWithMissingFields(iRowsWithMissingFields);
      processedDay.SetRowsWithFieldsErrors(iRowsWithFieldsErrors + processedDay.GetRowsWithFieldsErrors());

      return cleanList;
   }


   public void calculateBasicMetrics (ArrayList<JSONObject> lista, DayMetrics processedDay) {

      // This method does calculate basic metrics as TotalMessages, Blank Messages and OK/KO calls:
      Iterator<JSONObject> it = lista.iterator();
      Integer iRowsWithBlankMessages = 0;
      Integer iTotalMessages = 0;
      Integer iOKcalls = 0;
      Integer iKOcalls = 0;

      while(it.hasNext()){
         JSONObject item=it.next();
         if (item.get("message_type").equals(_MESSAGETYPE)) {
            iTotalMessages++;
            if (item.get("message_content").equals(""))
               iRowsWithBlankMessages++;               
            } else if (item.get("message_type").equals(_CALLTYPE)) {
               if (item.get("status_code").equals(_OKSTATUS))
                  iOKcalls++;
               else if (item.get("status_code").equals(_NOTOKSTATUS))
                  iKOcalls++;
            }
         }
      
      // Setting the results:
      processedDay.SetTotalNumOfMessages(iTotalMessages);
      processedDay.SetMessagesWithBlankContent(iRowsWithBlankMessages);
      processedDay.SetTotalNumOfCalls(iOKcalls+iKOcalls);
      processedDay.SetOKcallsPercentage((iOKcalls*100)/(iOKcalls+iKOcalls));
   }


   public ArrayList<JSONObject> calculateCCgroupedMetrics (ArrayList<JSONObject> lista, DayMetrics processedDay) {

      // This method calculate the metrics which are based on Country Codes:
      Iterator<JSONObject> it = lista.iterator();
      ArrayList<JSONObject> onlyMSGsList = new ArrayList<>();
      String MSISDNorigin;
      String MSISDNdestination;
      String CCorigin = "00";
      String CCdestination = "00";
      Integer averageCallsDuration = 0;

      Map <String, Integer> calls = new HashMap<String, Integer> ();
      ArrayList<String> destinationCCs = new ArrayList<>();
      ArrayList<String> originCCs = new ArrayList<>();
      Map <String, Integer> duration = new HashMap<String, Integer> ();

      while(it.hasNext()) {
      JSONObject item=it.next();

         if (item.get("message_type").equals(_CALLTYPE)) {
     
            MSISDNorigin = item.get("origin").toString();
            MSISDNdestination = item.get("destination").toString();
            CCorigin = MSISDNorigin.substring(0,2);
            CCdestination = MSISDNdestination.substring(0,2);
     
            if (calls.containsKey(CCorigin)) {
               // Average duration:
               averageCallsDuration = ((duration.get(CCorigin) * calls.get(CCorigin)) + Integer.parseInt(item.get("duration").toString())) / (calls.get(CCorigin)+1);
               duration.put(CCorigin, averageCallsDuration);
               // Number of calls:
               calls.put(CCorigin, calls.get(CCorigin)+1);
     
            } else {
               // Number of calls:
               calls.put(CCorigin, 1);
               // Average duration:
               duration.put(CCorigin, Integer.parseInt(item.get("duration").toString()));
            }
     
            if (!originCCs.contains(CCorigin))
               originCCs.add(CCorigin);
            if (!destinationCCs.contains(CCdestination))
               destinationCCs.add(CCdestination);
     
         } else if (item.get("message_type").equals(_MESSAGETYPE)) {
            onlyMSGsList.add(item);
         }
      }

      // Building the results:
      String strCallsByCC = "";

      for (Map.Entry<String, Integer> entry : calls.entrySet()) {
         strCallsByCC += ("\n      Country Code: " +entry.getKey() +" ; Total Calls Done: " +entry.getValue() +" ; Average Call Duration: " +duration.get(entry.getKey()) +"\'");
      }

      // Setting the results:
      processedDay.SetCallsGroupedByCC(strCallsByCC);
      processedDay.SetOriginCCsList(originCCs);
      processedDay.SetDestinationCCsList(destinationCCs);

      return onlyMSGsList;
    }


   public void performMessagesWordRanking(ArrayList<JSONObject> lista, DayMetrics processedDay) {

      // This method builds the ranking with the most frequent words in messages:
      Iterator<JSONObject> it = lista.iterator();

      Map <String, Integer> wordCount = new HashMap<String, Integer> ();
      String[] words;

      while(it.hasNext()) {
         JSONObject item=it.next();

         words = item.get("message_content").toString().split("\\s+");

         for (String word : words) {
            if (!Pattern.compile( "[0-9]" ).matcher(word).find() && !word.equals("")) {
               word = word.toLowerCase();
               word = word.replace("?", "");
               wordCount.compute(word, (k, v) -> v == null ? 1 : (v + 1));
            }
         }
      }

      // Sorting the results by value (word counter):            
      wordCount = wordCount.entrySet().stream().sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                     .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2,
                        LinkedHashMap::new));
      // Building the results:
      Iterator<?> itWC = wordCount.entrySet().iterator();
      int i = 0;
      String wordRanking = "";
      while (itWC.hasNext() && i <= TOP_WORDS-1) {
         Map.Entry<String, Integer> m = (Entry<String, Integer>) itWC.next();
         wordRanking += ("\n      TOP " +(i+1) +": " +m.getKey() +" [" +m.getValue() +"]");
         i++;
      }
      processedDay.SetWordRanking(wordRanking);
    }

}
