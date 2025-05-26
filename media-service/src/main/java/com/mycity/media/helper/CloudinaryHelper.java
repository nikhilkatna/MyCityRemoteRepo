package com.mycity.media.helper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Component
public class CloudinaryHelper {

    private final Cloudinary cloudinary;

    public CloudinaryHelper(
        @Value("${cloudinary.cloud_name}") String cloudName,
        @Value("${cloudinary.key}") String apiKey,
        @Value("${cloudinary.secret}") String apiSecret
    ) {
        this.cloudinary = new Cloudinary(
            ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
            )
        );
    }

    public String saveImage(MultipartFile image) {
        try {
            Map<String, Object> uploadOptions = new HashMap<>();
            uploadOptions.put("folder", "Events");

            Map<String, Object> uploadResult = cloudinary.uploader().upload(image.getBytes(), uploadOptions);
            return (String) uploadResult.get("url");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to upload image to Cloudinary", e);
        }
    }

    public List<String> saveImages(List<MultipartFile> files) {
        List<String> imageUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                Map<String, Object> uploadOptions = new HashMap<>();
                uploadOptions.put("folder", "Events");

                Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadOptions);
                String url = (String) uploadResult.get("url"); // ✅ Corrected here (no space)

                if (url != null) {
                    imageUrls.add(url);
                    System.out.println("Uploaded to Cloudinary URL: " + url); // Optional log
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return imageUrls;
    }


	public void deleteImage(String imageUrl) {
	    String publicId = extractPublicIdFromUrl(imageUrl);  // e.g., "foldername/imagename"
	    try {
			cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String saveVideo(MultipartFile video) {
	    try {
	        Map<String, Object> uploadOptions = new HashMap<>();
	        uploadOptions.put("folder", "Videos"); // You can customize the folder
	        uploadOptions.put("resource_type", "video"); // Required for video

	        Map<String, Object> uploadResult = cloudinary.uploader().upload(video.getBytes(), uploadOptions);
	        return (String) uploadResult.get("url");
	    } catch (IOException e) {
	        e.printStackTrace();
	        throw new RuntimeException("Failed to upload video to Cloudinary", e);
	    }
	}


	private String extractPublicIdFromUrl(String url) {
	    // Assuming URL format: https://res.cloudinary.com/your-cloud-name/image/upload/v123456789/folder/imagename.jpg
	    // Extract "folder/imagename" (without extension)
	    String[] parts = url.split("/");
	    String filename = parts[parts.length - 1];
	    String folder = parts[parts.length - 2];
	    return folder + "/" + filename.split("\\.")[0];
	}



}
