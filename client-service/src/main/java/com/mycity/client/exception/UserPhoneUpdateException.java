package com.mycity.client.exception;

public class UserPhoneUpdateException extends RuntimeException 
{
  public UserPhoneUpdateException(String message)
  {
	  super(message);
  }
  
  public UserPhoneUpdateException(String message,Throwable cause)
  {
	  super(message,cause);
  }
}
