package com.mycity.client.exception;

public class MissingTokenException extends RuntimeException 
{
  public MissingTokenException(String message)
  {
	  super(message);
  }
}
