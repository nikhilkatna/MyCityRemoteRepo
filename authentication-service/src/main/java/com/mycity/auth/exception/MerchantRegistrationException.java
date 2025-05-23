package com.mycity.auth.exception;

public class MerchantRegistrationException extends RuntimeException 
{
  public MerchantRegistrationException(String message)
  {
	  super(message);
  }
  
  public MerchantRegistrationException(String message,Throwable cause)
  {
	  super(message,cause);
  }
}
