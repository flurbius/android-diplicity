package se.oort.diplicity.apigen;

import retrofit2.http.*;
	
public class Order implements java.io.Serializable {
  public String GameID;
  public Long PhaseOrdinal;
  public String Nation;
  public java.util.List<String> Parts;
}