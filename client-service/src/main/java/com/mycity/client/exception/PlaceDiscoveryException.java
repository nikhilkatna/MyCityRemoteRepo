package com.mycity.client.exception;

public class PlaceDiscoveryException extends RuntimeException 
{
   public PlaceDiscoveryException(String message)
   {
	   super(message);
   }
   
   public PlaceDiscoveryException(String message, Throwable cause) {
       super(message, cause);
   }
}
