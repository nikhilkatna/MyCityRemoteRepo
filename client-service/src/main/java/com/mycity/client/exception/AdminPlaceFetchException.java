package com.mycity.client.exception;

public class AdminPlaceFetchException extends RuntimeException 
{
  public AdminPlaceFetchException(String message)
  {
	  super(message);
  }
  
  public AdminPlaceFetchException(String message,Throwable cause)
  {
	  super(message,cause);
  }
}
