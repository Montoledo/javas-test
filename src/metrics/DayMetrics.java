package metrics;

import java.util.ArrayList;

public class DayMetrics {

   private String id;
   private Integer iTotalNumOfRows;
   private Integer iTotalNumOfCalls;
   private Integer iTotalNumOfMessages;
   private Integer iRowsWithMissingFields;
   private Integer iMessagesWithBlankContent;
   private Integer iRowsWithFieldsErrors;
   private Integer iOKcallsPercentage;
   private ArrayList<String> OriginCCs;
   private ArrayList<String> DestinationCCs;
   private String strCallsGroupedByCC;
   private String strWordRanking;

   public DayMetrics(String date) {
      this.id = date;
   }

   // Set & Get methods:
   public void SetTotalNumOfRows (Integer value) {
      this.iTotalNumOfRows = value;
   }
   public void SetTotalNumOfCalls (Integer value) {
      this.iTotalNumOfCalls = value;
   }
   public void SetTotalNumOfMessages (Integer value) {
      this.iTotalNumOfMessages = value;
   }
   public void SetRowsWithMissingFields (Integer value) {
      this.iRowsWithMissingFields = value;
   }
   public void SetMessagesWithBlankContent (Integer value) {
      this.iMessagesWithBlankContent = value;
   }
   public void SetRowsWithFieldsErrors (Integer value) {
      this.iRowsWithFieldsErrors = value;
   }
   public void SetOKcallsPercentage (Integer value) {
      this.iOKcallsPercentage = value;
   }
   public void SetCallsGroupedByCC (String value) {
      this.strCallsGroupedByCC = value;
   }
   public void SetWordRanking (String value) {
      this.strWordRanking = value;
   }
   public void SetOriginCCsList (ArrayList<String> list) {
      this.OriginCCs = list;
   }
   public void SetDestinationCCsList (ArrayList<String> list) {
      this.DestinationCCs = list;
   }

   public Integer GetTotalNumOfRows() {
      return iTotalNumOfRows;
   }
   public Integer GetTotalNumOfCalls() {
      return iTotalNumOfCalls;
   }
   public Integer GetTotalNumOfMessages() {
      return iTotalNumOfMessages;
   }
   public Integer GetRowsWithMissingFields() {
      return iRowsWithMissingFields;
   }
   public Integer GetMessagesWithBlankContent() {
      return this.iMessagesWithBlankContent;
   }
   public Integer GetRowsWithFieldsErrors() {
      return this.iRowsWithFieldsErrors;
   }
   public Integer GetOKcallsPercentage() {
      return this.iOKcallsPercentage;
   }
   public String GetCallsGroupedByCC() {
      return this.strCallsGroupedByCC;
   }
   public String GetWordRanking() {
      return this.strWordRanking;
   }
   public ArrayList<String> GetOriginCCsList() {
      return this.OriginCCs;
   }
   public ArrayList<String> GetDestinationCCsList() {
      return this.DestinationCCs;
   }

   public String GetId() {
     return this.id;
   }
}
