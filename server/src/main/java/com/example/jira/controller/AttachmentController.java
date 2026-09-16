package com.example.jira.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.jira.model.Attachment;
import com.example.jira.service.AttachmentService;

@RestController
@RequestMapping("/api/attachments")
@CrossOrigin(origins = "*")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(
            AttachmentService attachmentService) {

        this.attachmentService = attachmentService;
    }

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Attachment> upload(
            @RequestParam("issueId") String issueId,
            @RequestParam("userId") String userId,
            @RequestParam("file") MultipartFile file)
            throws IOException {

        return ResponseEntity.ok(
                attachmentService.upload(
                        issueId,
                        userId,
                        file
                )
        );
    }

    @GetMapping("/issue/{issueId}")
    public ResponseEntity<List<Attachment>> getByIssue(
            @PathVariable String issueId) {

        return ResponseEntity.ok(
                attachmentService.getByIssue(issueId)
        );
    }

    @GetMapping("/file/{fileId}/download")
public ResponseEntity<Resource> download(
        @PathVariable String fileId) {

    var resource = attachmentService.downloadByFileId(fileId);

    var attachment = attachmentService
            .findByFileId(fileId);

    return ResponseEntity.ok()
            .contentType(
                    MediaType.parseMediaType(
                            attachment.getContentType()
                    )
            )
            .header(
                    HttpHeaders.CONTENT_DISPOSITION,
                    ContentDisposition
                            .attachment()
                            .filename(
                                    attachment.getOriginalFilename()
                            )
                            .build()
                            .toString()
            )
            .body(resource);
}

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable String id) {

        attachmentService.delete(id);

        return ResponseEntity.noContent().build();
    }
}