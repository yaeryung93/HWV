package com.example.backend.service;

import com.example.backend.entity.PdfFile;
import com.example.backend.repository.PdfFileRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Service
public class PdfService {

    private final PdfFileRepository pdfRepository;

    public PdfService(PdfFileRepository pdfRepository) {
        this.pdfRepository = pdfRepository;
    }

    public PdfFile upload(MultipartFile file) throws IOException {

        String uploadDir = "uploads/";

        File dir = new File(uploadDir);

        if (!dir.exists()) {
            dir.mkdirs();
        }

        String path = uploadDir + file.getOriginalFilename();

        file.transferTo(new File(path));

        PdfFile pdf = new PdfFile();

        pdf.setFileName(file.getOriginalFilename());
        pdf.setFilePath(path);

        return pdfRepository.save(pdf);
    }
}