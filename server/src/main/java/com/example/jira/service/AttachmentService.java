package com.example.jira.service;

import java.io.IOException;
import java.util.List;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.jira.model.Attachment;
import com.example.jira.repository.AttachmentRepository;

@Service
public class AttachmentService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private static final List<String> ALLOWED_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "application/pdf",
            "text/plain",
            "application/zip"
    );

    private final GridFsTemplate gridFsTemplate;
    private final AttachmentRepository attachmentRepository;

    public AttachmentService(
            GridFsTemplate gridFsTemplate,
            AttachmentRepository attachmentRepository) {

        this.gridFsTemplate = gridFsTemplate;
        this.attachmentRepository = attachmentRepository;
    }
    public Attachment findByFileId(String fileId) {
    return attachmentRepository
            .findAll()
            .stream()
            .filter(a -> fileId.equals(a.getFileId()))
            .findFirst()
            .orElseThrow(() ->
                    new IllegalArgumentException("Attachment not found")
            );
}

public GridFsResource downloadByFileId(String fileId) {

    if (!ObjectId.isValid(fileId)) {
        throw new IllegalArgumentException("Invalid file ID");
    }

    GridFSFile file = gridFsTemplate.findOne(
            Query.query(
                    Criteria.where("_id")
                            .is(new ObjectId(fileId))
            )
    );

    if (file == null) {
        throw new IllegalArgumentException("File not found");
    }

    return gridFsTemplate.getResource(file);
}

    public Attachment upload(
            String issueId,
            String userId,
            MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "File cannot be empty"
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "File size cannot exceed 10 MB"
            );
        }

        String contentType = file.getContentType();

        if (contentType == null
                || !ALLOWED_TYPES.contains(contentType)) {

            throw new IllegalArgumentException(
                    "Unsupported file type"
            );
        }

        String filename = file.getOriginalFilename();

        if (filename == null || filename.isBlank()) {
            filename = "attachment";
        }

        filename = filename.replaceAll(
                "[^a-zA-Z0-9._-]",
                "_"
        );

        ObjectId fileId = gridFsTemplate.store(
                file.getInputStream(),
                filename,
                contentType,
                new Document("issueId", issueId)
                        .append("uploadedBy", userId)
        );

        Attachment attachment = new Attachment();

        attachment.setIssueId(issueId);
        attachment.setFileId(fileId.toHexString());
        attachment.setOriginalFilename(filename);
        attachment.setContentType(contentType);
        attachment.setSize(file.getSize());
        attachment.setUploadedBy(userId);

        return attachmentRepository.save(attachment);
    }

    public List<Attachment> getByIssue(String issueId) {

        return attachmentRepository
                .findByIssueIdOrderByUploadedAtDesc(issueId);
    }

    public GridFsResource download(String id) {

        Attachment attachment = findAttachment(id);

        GridFSFile file = findGridFsFile(
                attachment.getFileId()
        );

        if (file == null) {
            throw new IllegalArgumentException(
                    "File not found"
            );
        }

        return gridFsTemplate.getResource(file);
    }

    public void delete(String id) {

        Attachment attachment = findAttachment(id);

        if (ObjectId.isValid(attachment.getFileId())) {

            gridFsTemplate.delete(
                    Query.query(
                            Criteria.where("_id")
                                    .is(new ObjectId(
                                            attachment.getFileId()
                                    ))
                    )
            );
        }

        attachmentRepository.deleteById(
                attachment.getId()
        );
    }

    public Attachment findAttachment(String id) {

        if (!ObjectId.isValid(id)) {
            throw new IllegalArgumentException(
                    "Invalid attachment ID"
            );
        }

        return attachmentRepository
                .findById(new ObjectId(id))
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Attachment not found"
                        )
                );
    }

    private GridFSFile findGridFsFile(String fileId) {

        if (!ObjectId.isValid(fileId)) {
            throw new IllegalArgumentException(
                    "Invalid file ID"
            );
        }

        return gridFsTemplate.findOne(
                Query.query(
                        Criteria.where("_id")
                                .is(new ObjectId(fileId))
                )
        );
    }
}