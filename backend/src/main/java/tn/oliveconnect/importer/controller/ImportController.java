package tn.oliveconnect.importer.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.oliveconnect.common.enums.ImportType;
import tn.oliveconnect.common.web.PageResponse;
import tn.oliveconnect.importer.dto.ImportJobResponse;
import tn.oliveconnect.importer.service.ImportService;

@RestController
@RequestMapping("/api/v1/admin/imports")
@PreAuthorize("hasRole('ADMIN')")
public class ImportController {
    private final ImportService service;
    public ImportController(ImportService service) { this.service = service; }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ImportJobResponse upload(@RequestParam ImportType type, @RequestPart("file") MultipartFile file) {
        return service.importMultipart(type, file);
    }

    @GetMapping
    public PageResponse<ImportJobResponse> list(
            @PageableDefault(size = 20, sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(pageable);
    }

    @GetMapping("/{id}")
    public ImportJobResponse get(@PathVariable String id) {
        return service.get(id);
    }
}
