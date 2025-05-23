package com.mycity.admin.exception;

public class InvalidCredentialsException extends RuntimeException 
{
  public InvalidCredentialsException(String message)
  {
	  super(message);
  }
}
