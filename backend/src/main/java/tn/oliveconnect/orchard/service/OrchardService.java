package tn.oliveconnect.orchard.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;
import tn.oliveconnect.common.exception.ResourceNotFoundException;
import tn.oliveconnect.orchard.document.GeoLocation;
import tn.oliveconnect.orchard.document.MaturityStatus;
import tn.oliveconnect.orchard.document.OrchardDocument;
import tn.oliveconnect.orchard.dto.CreateOrchardRequest;
import tn.oliveconnect.orchard.dto.OrchardResponse;
import tn.oliveconnect.orchard.repository.OrchardRepository;
import tn.oliveconnect.security.UserPrincipal;
import tn.oliveconnect.user.document.Role;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrchardService {

    private final OrchardRepository orchardRepository;

    public OrchardResponse createOrchard(CreateOrchardRequest request, UserPrincipal principal) {
        GeoJsonPoint geoPoint = new GeoJsonPoint(
                request.getLocation().getLongitude(),
                request.getLocation().getLatitude()
        );

        OrchardDocument orchard = OrchardDocument.builder()
                .farmerId(principal.getId())
                .name(request.getName().trim())
                .surfaceHectares(request.getSurfaceHectares())
                .treeCount(request.getTreeCount())
                .oliveVariety(request.getOliveVariety().trim())
                .location(geoPoint)
                .maturityStatus(request.getMaturityStatus() != null ? request.getMaturityStatus() : MaturityStatus.UNKNOWN)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        OrchardDocument saved = orchardRepository.save(orchard);
        return toResponse(saved);
    }

    public List<OrchardResponse> getOrchards(UserPrincipal principal) {
        List<OrchardDocument> list;
        if (principal.getRole() == Role.FARMER) {
            list = orchardRepository.findByFarmerId(principal.getId());
        } else {
            list = orchardRepository.findAll();
        }
        return list.stream().map(this::toResponse).toList();
    }

    public OrchardResponse getOrchardById(String id, UserPrincipal principal) {
        OrchardDocument orchard;
        if (principal.getRole() == Role.FARMER) {
            orchard = orchardRepository.findByIdAndFarmerId(id, principal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Verger introuvable : " + id));
        } else {
            orchard = orchardRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Verger introuvable : " + id));
        }
        return toResponse(orchard);
    }

    public OrchardResponse toResponse(OrchardDocument doc) {
        GeoLocation loc = null;
        if (doc.getLocation() != null) {
            loc = GeoLocation.builder()
                    .longitude(doc.getLocation().getX())
                    .latitude(doc.getLocation().getY())
                    .build();
        }

        return OrchardResponse.builder()
                .id(doc.getId())
                .farmerId(doc.getFarmerId())
                .name(doc.getName())
                .surfaceHectares(doc.getSurfaceHectares())
                .treeCount(doc.getTreeCount())
                .oliveVariety(doc.getOliveVariety())
                .location(loc)
                .maturityStatus(doc.getMaturityStatus())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
