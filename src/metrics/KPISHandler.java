package metrics;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

class KPISHandler implements HttpHandler {

   public void handle(HttpExchange t) throws IOException {
      String response = "";
      response = elaborateKPIcounters();
      t.sendResponseHeaders(200, response.length());
      OutputStream os = t.getResponseBody();
      os.write(response.getBytes());
      os.close();
   }


   public String elaborateKPIcounters() {

      // Method for the elaboration of the KPI counters:
      KPIcounters kpi = null;
      String filename = "resources/KPIcounters.dat";
      ClassLoader classLoader = ClassLoader.getSystemClassLoader();
      File kpifile = new File(classLoader.getResource(filename).getFile());

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

         // Close up of the Input streams:
         oi.close();
         fi.close();

      } catch (FileNotFoundException e) {
         System.out.println("File not found");
      } catch (IOException e) {
         System.out.println("KPIcounters file is empty");
         e.printStackTrace();
      } catch (ClassNotFoundException e) {
         System.out.println("KPIcounters file is empty");
         e.printStackTrace();
      }

      // Retrieving the KPI data from the file in order to elaborate the report: 
      String sheet = "KPIs Report:\n------------";

      sheet += ("\n   Total number of processed days: " +kpi.GetTotalProcessedDays());
      sheet += ("\n   Total number of rows: " +kpi.GetTotalRows());
      sheet += ("\n   Total number of calls: " +kpi.GetTotalCalls());
      sheet += ("\n   Total number of messages: " +kpi.GetTotalMessages());
      sheet += ("\n   Total number of unique Originating Country Codes: " +kpi.GetTotalDiffOriginCCs());
      sheet += ("\n   Total number of unique Destination Country Codes: " +kpi.GetTotalDiffDestinationCCs());
      sheet += ("\n   Average time of processing each day : " +kpi.GetAvgProcessTime() +"ms");

      return sheet;

    }

}
