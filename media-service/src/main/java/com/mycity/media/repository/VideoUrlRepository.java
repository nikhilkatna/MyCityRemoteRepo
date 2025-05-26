package com.mycity.media.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mycity.media.entity.VideoDetailsResponse;
import com.mycity.media.entity.Videos;

public interface VideoUrlRepository extends JpaRepository<Videos,Long> 
{
	@Query("SELECT v.monthName, v.placeName, v.videoUrl FROM Videos v WHERE v.monthName = :monthName")
	List<Object[]> findRawVideoDetailsByMonth(@Param("monthName") String monthName);

	@Query("SELECT DISTINCT v.videoUrl FROM Videos v WHERE v.monthName = :monthName")
	List<String> findAllVideoUrlsByMonthName(@Param("monthName") String monthName);


}
