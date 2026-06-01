package com.amalia.harmonyhub_backend.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret) {
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret
        ));
    }

    public String uploadBase64Image(String base64Data) {
        try {
            String imageData = base64Data;
            if (base64Data.contains(",")) {
                imageData = base64Data.split(",")[1];
            }

            Map result = cloudinary.uploader().upload(
                    "data:image/jpeg;base64," + imageData,
                    ObjectUtils.asMap("folder", "harmonyhub")
            );
            return (String) result.get("secure_url");
        } catch (Exception e) {
            System.err.println("Cloudinary upload failed: " + e.getMessage());
            return null;
        }
    }
}