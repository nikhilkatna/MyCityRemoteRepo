package com.mycity.media.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class MediaSecurityConfig {
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
	    http
	        .csrf(csrf -> csrf.disable())
	        .authorizeHttpRequests(auth -> auth
	            .requestMatchers("/media/get-image/{userId}","/media/admin/get-image/**","/media/admin/upload","/media/upload/image","/media/images/**","/media/upload","/media/cover-image","/media/fetch{id}","/media/images/{placeId}","/media/images/delete/{placeId}",
	            		         "/media/review/upload","/media/review/delete/{reviewId}","/media/bycategory/image","/media/findby/{placeId}",
	            		         "/media/gallery/upload","media/gallery/getimages/{districtName}","/media/gallery/deleteimage/{imageId}","/media/image-byplacename/{placeName}","/media/upload/places","/media/upload/cuisines","/media/upload/images", "/media/delete/images/**", "/media/update/images/**", "/media/fetch/images/**","/media/review/image/place/**","/media/images/image-byplacename/{placeName}")
	            .permitAll()
	            .anyRequest().authenticated()
	        );
	    return http.build();
	}
	
	 @Bean
	    public CorsWebFilter corsWebFilter() {
	        CorsConfiguration config = new CorsConfiguration();
	        config.addAllowedOrigin("*"); // TODO: For production, replace * with trusted domains
	        config.addAllowedHeader("*");
	        config.addAllowedMethod("*");
	        config.setAllowCredentials(false);

	        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
	        source.registerCorsConfiguration("/**", config);
	        return new CorsWebFilter(source);
	    }
	    
}