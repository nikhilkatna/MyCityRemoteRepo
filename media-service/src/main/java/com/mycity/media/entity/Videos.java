package com.mycity.media.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Videos 
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long videoId;
	
	@Column(unique = true)
    private String placeName;
	
    private String monthName;
    
    private String videoUrl;
}
