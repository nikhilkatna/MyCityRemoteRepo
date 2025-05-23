package com.mycity.admin.exception;

public class CustomClientException extends RuntimeException 
{
   public CustomClientException(String message)
   {
	   super(message);
   }
}
