package tn.oliveconnect.harvest.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.oliveconnect.common.exception.ResourceNotFoundException;
import tn.oliveconnect.harvest.document.HarvestRequestDocument;
import tn.oliveconnect.harvest.document.HarvestStatus;
import tn.oliveconnect.harvest.dto.CreateHarvestRequest;
import tn.oliveconnect.harvest.dto.HarvestRequestResponse;
import tn.oliveconnect.harvest.repository.HarvestRequestRepository;
import tn.oliveconnect.orchard.repository.OrchardRepository;
import tn.oliveconnect.security.UserPrincipal;
import tn.oliveconnect.user.document.Role;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HarvestRequestService {

    private final HarvestRequestRepository harvestRequestRepository;
    private final OrchardRepository orchardRepository;

    public HarvestRequestResponse createHarvestRequest(CreateHarvestRequest request, UserPrincipal principal) {
        // Ownership check: orchard must belong to this farmer
        orchardRepository.findByIdAndFarmerId(request.getOrchardId(), principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Verger introuvable ou non autorisé : " + request.getOrchardId()));

        HarvestRequestDocument document = HarvestRequestDocument.builder()
                .orchardId(request.getOrchardId())
                .farmerId(principal.getId())
                .type(request.getType())
                .estimatedQuantityKg(request.getEstimatedQuantityKg())
                .desiredStartDate(request.getDesiredStartDate())
                .status(HarvestStatus.SUBMITTED)
                .notes(request.getNotes())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        HarvestRequestDocument saved = harvestRequestRepository.save(document);
        return toResponse(saved);
    }

    public List<HarvestRequestResponse> getHarvestRequests(UserPrincipal principal) {
        List<HarvestRequestDocument> list;
        if (principal.getRole() == Role.FARMER) {
            list = harvestRequestRepository.findByFarmerId(principal.getId());
        } else {
            list = harvestRequestRepository.findAll();
        }
        return list.stream().map(this::toResponse).toList();
    }

    public HarvestRequestResponse getHarvestRequestById(String id, UserPrincipal principal) {
        HarvestRequestDocument doc;
        if (principal.getRole() == Role.FARMER) {
            doc = harvestRequestRepository.findByIdAndFarmerId(id, principal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Demande de récolte introuvable : " + id));
        } else {
            doc = harvestRequestRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Demande de récolte introuvable : " + id));
        }
        return toResponse(doc);
    }

    public HarvestRequestResponse toResponse(HarvestRequestDocument doc) {
        return HarvestRequestResponse.builder()
                .id(doc.getId())
                .orchardId(doc.getOrchardId())
                .farmerId(doc.getFarmerId())
                .type(doc.getType())
                .estimatedQuantityKg(doc.getEstimatedQuantityKg())
                .desiredStartDate(doc.getDesiredStartDate())
                .status(doc.getStatus())
                .notes(doc.getNotes())
                .rejectionReason(doc.getRejectionReason())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
