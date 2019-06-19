package metrics;

import java.io.Serializable;
import java.util.ArrayList;

public class KPIcounters implements Serializable {

   private static final long serialVersionUID = 1L;
   private ArrayList<String> ProcessedDates;
   private Integer iTotalRows = new Integer(0);
   private Integer iTotalCalls = new Integer(0);
   private Integer iTotalMessages = new Integer(0);
   private ArrayList<String> originCCs;
   private ArrayList<String> destinationCCs;
   private long iAvgProcessTime = 0;


   // Set & Get methods:
   public void AddProcessedDate (String date) {
      if (this.ProcessedDates == null) {
         this.ProcessedDates = new ArrayList<String>();
      }
      this.ProcessedDates.add(date);
   }
   public void AddTotalRows (Integer value) {
      if (iTotalRows != null)
         this.iTotalRows += value;
      else
         this.iTotalRows = value;
   }
   public void AddTotalCalls (Integer value) {
      if (iTotalCalls != null)
         this.iTotalCalls += value;
      else
         this.iTotalCalls = value;
   }
   public void AddTotalMessages (Integer value) {
      if (iTotalMessages != null)
         this.iTotalMessages += value;
      else
         this.iTotalMessages = value;
   }
   public void AddOriginCC (ArrayList<String> listToAdd) {
      if (this.originCCs != null) {
         listToAdd.removeAll(this.originCCs);
         this.originCCs.addAll(listToAdd);
      } else { this.originCCs = listToAdd; }
   }
   public void AddDestinationCC (ArrayList<String> listToAdd) {
      if (this.destinationCCs != null) {
         listToAdd.removeAll(this.destinationCCs);
         this.destinationCCs.addAll(listToAdd);
      } else { this.destinationCCs = listToAdd; }
   }
   public void AddProcessTime (long value) {      
      if (iAvgProcessTime == 0)
         iAvgProcessTime = value;
      else
         this.iAvgProcessTime = ((GetAvgProcessTime() * GetTotalProcessedDays()) + value) / (GetTotalProcessedDays()+1);
   }


   public Boolean ContainsDate(String date) {
      if (ProcessedDates != null)
         return this.ProcessedDates.contains(date);
      return false;
   }
   public Integer GetTotalProcessedDays() {
      if (ProcessedDates != null) 
         return this.ProcessedDates.size();
      return 0;
   }
   public Integer GetTotalRows() {
      return this.iTotalRows;
   }
   public Integer GetTotalCalls() {
      return iTotalCalls;
   }
   public Integer GetTotalMessages() {
      return iTotalMessages;
   }
   public Boolean ContainsOriginCC(String CC) {
      return this.originCCs.contains(CC);
   }
   public Integer GetTotalDiffOriginCCs() {
      if (this.originCCs != null)
         return this.originCCs.size();
      return 0;
   }
   public Boolean ContainsDestinationCC(String CC) {
      return this.destinationCCs.contains(CC);
   }
   public Integer GetTotalDiffDestinationCCs() {
      if (this.destinationCCs != null)
         return this.destinationCCs.size();
      return 0;
   }
   public long GetAvgProcessTime() {
      return iAvgProcessTime;
   }
}
