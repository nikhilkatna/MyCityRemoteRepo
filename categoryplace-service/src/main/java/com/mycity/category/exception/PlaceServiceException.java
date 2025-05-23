package com.mycity.category.exception;

public class PlaceServiceException extends RuntimeException 
{
  public PlaceServiceException(String message)
  {
	  super(message);
  }
  
  public PlaceServiceException(String message,Throwable cause)
  {
	  super(message,cause);
  }
}
