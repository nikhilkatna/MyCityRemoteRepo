package com.mycity.category.exception;

public class CategoryOperationException extends RuntimeException 
{
  public CategoryOperationException(String message,Throwable cause)
  {
	  super(message,cause);
  }
}
