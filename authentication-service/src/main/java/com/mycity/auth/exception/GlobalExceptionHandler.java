package com.mycity.auth.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.mycity.auth.controller.AuthOtpController;
import com.mycity.shared.errordto.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {
	
	private static final Logger logger=LoggerFactory.getLogger(GlobalExceptionHandler.class);
	
	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<String> handleUserNotFound(UserNotFoundException ex) {
	    logger.warn("User not found: {}", ex.getMessage());
	    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
	}

	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<String> handleGenericRuntime(RuntimeException ex) {
	    logger.error("Internal server error: {}", ex.getMessage(), ex);
	    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	            .body("Internal error: " + ex.getMessage());
	}
	
	 @ExceptionHandler(AuthenticationException.class)
	    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
		 logger.warn("Authentication failure: {}", ex.getMessage());
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                .body(new ErrorResponse(ex.getMessage(), 401));
	 }
	 
	 @ExceptionHandler(LogoutException.class)
	    public ResponseEntity<ErrorResponse> handleLogoutException(LogoutException ex) {
	        logger.error("Logout exception: {}", ex.getMessage());
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body(new ErrorResponse(ex.getMessage(), 500));
	 }
	 

     @ExceptionHandler(EmailServiceException.class)
	 public ResponseEntity<ErrorResponse> handleEmailServiceException(EmailServiceException ex) {
	        logger.error("EmailServiceException: {}", ex.getMessage());
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	           .body(new ErrorResponse(ex.getMessage(), 500));
	 }

	 @ExceptionHandler(OtpServiceException.class)
      public ResponseEntity<ErrorResponse> handleOtpServiceException(OtpServiceException ex) {
	        logger.error("OtpServiceException: {}", ex.getMessage());
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	            .body(new ErrorResponse(ex.getMessage(), 500));
	 }
	 
	  @ExceptionHandler(UserRegistrationException.class)
	    public ResponseEntity<ErrorResponse> handleUserRegistrationException(UserRegistrationException ex) {
	        logger.error("UserRegistrationException: {}", ex.getMessage());
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	            .body(new ErrorResponse(ex.getMessage(), 500));
	    }

	    @ExceptionHandler(MerchantRegistrationException.class)
	    public ResponseEntity<ErrorResponse> handleMerchantRegistrationException(MerchantRegistrationException ex) {
	        logger.error("MerchantRegistrationException: {}", ex.getMessage());
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	            .body(new ErrorResponse(ex.getMessage(), 500));
	    }

}
