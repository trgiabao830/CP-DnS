package com.tgb.cp_dns.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;

    public String uploadImage(MultipartFile file, String nameBase, String folderName) throws IOException {

        String baseFileName = formatFileName(nameBase);

        String uniqueFileName = baseFileName + "-" + UUID.randomUUID().toString();

        Map<String, Object> options = new HashMap<>();
        options.put("folder", folderName);
        options.put("public_id", uniqueFileName);
        options.put("resource_type", "image");

        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), options);
        return result.get("url").toString();
    }

    private String formatFileName(String name) {
        if (name == null)
            return "item";
        String temp = Normalizer.normalize(name.toLowerCase(), Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String noAccent = pattern.matcher(temp).replaceAll("");
        return noAccent.replaceAll(" ", "-").replaceAll("[^a-z0-9-]", "");
    }

    public void deleteImage(String publicId) {
        if (publicId != null && !publicId.isEmpty()) {
            try {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            } catch (IOException e) {
                System.err.println("Lỗi xóa ảnh Cloudinary: " + e.getMessage());
            }
        }
    }

    public String getPublicIdFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty())
            return null;

        try {
            int uploadIndex = imageUrl.indexOf("upload/");
            if (uploadIndex == -1)
                return null;

            String temp = imageUrl.substring(uploadIndex + 7);

            int slashIndex = temp.indexOf("/");
            if (slashIndex != -1) {
                temp = temp.substring(slashIndex + 1);
            }

            int extensionIndex = temp.lastIndexOf(".");
            if (extensionIndex != -1) {
                return temp.substring(0, extensionIndex);
            }

            return temp;
        } catch (Exception e) {
            return null;
        }
    }
}