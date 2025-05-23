package com.mycity.client.exception;

public class AdminProfileException extends RuntimeException
{
  public AdminProfileException(String message)
  {
	  super(message);
  }
  
  public AdminProfileException(String message,Throwable cause)
  {
	  super(message,cause);
  }
}
