package com.mycity.category.exception;

public class CategoryAlreadyExistsException extends RuntimeException
{
   public CategoryAlreadyExistsException(String message)
   {
	   super(message);
   }
}
