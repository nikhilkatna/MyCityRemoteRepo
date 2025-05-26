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
public class Images 
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long imageId;

	private String imageUrl;
	@Column(name = "place_name") 
	private String placeName; // You might still want to store the name here

	private String category;

	@Column(name = "place_id")
	private Long placeId; // Storing the ID of the place from the other service

	private String imageName;
}
