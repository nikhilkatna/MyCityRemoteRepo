package com.mycity.shared.userdto;

import lombok.Data;

@Data
public class UserRegRequest {
 
	
	private String firstname;
	
	private String lastname;
		
	private String email;
    
    private String password;
    
    private String mobilenumber;
    
    private boolean OtpVerified;
    
}
