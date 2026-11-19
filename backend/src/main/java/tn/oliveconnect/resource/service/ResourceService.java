package tn.oliveconnect.resource.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import tn.oliveconnect.common.enums.ResourceStatus;
import tn.oliveconnect.common.error.ApiException;
import tn.oliveconnect.common.error.ErrorCode;
import tn.oliveconnect.common.web.PageResponse;
import tn.oliveconnect.resource.document.*;
import tn.oliveconnect.resource.dto.ResourceDtos.*;
import tn.oliveconnect.resource.repository.*;

@Service
public class ResourceService {

    private final WorkerRepository workers;
    private final VehicleRepository vehicles;
    private final BinRepository bins;
    private final EquipmentRepository equipment;
    private final ObjectMapper mapper;
    private final Validator validator;

    public ResourceService(WorkerRepository workers, VehicleRepository vehicles,
                           BinRepository bins, EquipmentRepository equipment,
                           ObjectMapper mapper, Validator validator) {
        this.workers = workers;
        this.vehicles = vehicles;
        this.bins = bins;
        this.equipment = equipment;
        this.mapper = mapper;
        this.validator = validator;
    }

    public PageResponse<?> list(String kind, ResourceStatus status, Pageable pageable) {
        return switch (kind) {
            case "workers" -> PageResponse.from(
                    status == null ? workers.findAll(pageable) : workers.findByStatus(status, pageable),
                    WorkerResponse::from);
            case "vehicles" -> PageResponse.from(
                    status == null ? vehicles.findAll(pageable) : vehicles.findByStatus(status, pageable),
                    VehicleResponse::from);
            case "bins" -> PageResponse.from(
                    status == null ? bins.findAll(pageable) : bins.findByStatus(status, pageable),
                    BinResponse::from);
            case "equipment" -> PageResponse.from(
                    status == null ? equipment.findAll(pageable) : equipment.findByStatus(status, pageable),
                    EquipmentResponse::from);
            default -> throw ApiException.validation("kind", "Doit valoir workers, vehicles, bins ou equipment");
        };
    }

    public Object create(String kind, JsonNode body) {
        return switch (kind) {
            case "workers" -> {
                WorkerRequest req = parseAndValidate(body, WorkerRequest.class);
                WorkerDocument d = new WorkerDocument();
                d.setFullName(req.fullName().trim());
                d.setPhone(req.phone().trim());
                d.setSkills(req.skills());
                d.setStatus(ResourceStatus.AVAILABLE);
                yield WorkerResponse.from(workers.save(d));
            }
            case "vehicles" -> {
                VehicleRequest req = parseAndValidate(body, VehicleRequest.class);
                VehicleDocument d = new VehicleDocument();
                d.setType(req.type());
                d.setRegistration(req.registration().trim());
                d.setCapacityKg(req.capacityKg());
                d.setStatus(ResourceStatus.AVAILABLE);
                yield VehicleResponse.from(vehicles.save(d));
            }
            case "bins" -> {
                BinRequest req = parseAndValidate(body, BinRequest.class);
                BinDocument d = new BinDocument();
                d.setCode(req.code().trim());
                d.setCapacityKg(req.capacityKg());
                d.setStatus(ResourceStatus.AVAILABLE);
                yield BinResponse.from(bins.save(d));
            }
            case "equipment" -> {
                EquipmentRequest req = parseAndValidate(body, EquipmentRequest.class);
                EquipmentDocument d = new EquipmentDocument();
                d.setType(req.type());
                d.setLabel(req.label().trim());
                d.setQuantity(req.quantity());
                d.setStatus(ResourceStatus.AVAILABLE);
                yield EquipmentResponse.from(equipment.save(d));
            }
            default -> throw ApiException.validation("kind", "Doit valoir workers, vehicles, bins ou equipment");
        };
    }

    public Object patch(String kind, String id, JsonNode body) {
        return switch (kind) {
            case "workers" -> {
                WorkerPatch req = parseAndValidate(body, WorkerPatch.class);
                WorkerDocument d = workers.findById(id).orElseThrow(() -> ApiException.notFound("Ouvrier introuvable."));
                if (req.fullName() != null) d.setFullName(req.fullName().trim());
                if (req.phone() != null) d.setPhone(req.phone().trim());
                if (req.skills() != null) d.setSkills(req.skills());
                if (req.status() != null) d.setStatus(req.status());
                yield WorkerResponse.from(workers.save(d));
            }
            case "vehicles" -> {
                VehiclePatch req = parseAndValidate(body, VehiclePatch.class);
                VehicleDocument d = vehicles.findById(id).orElseThrow(() -> ApiException.notFound("Véhicule introuvable."));
                if (req.type() != null) d.setType(req.type());
                if (req.registration() != null) d.setRegistration(req.registration().trim());
                if (req.capacityKg() != null) d.setCapacityKg(req.capacityKg());
                if (req.status() != null) d.setStatus(req.status());
                yield VehicleResponse.from(vehicles.save(d));
            }
            case "bins" -> {
                BinPatch req = parseAndValidate(body, BinPatch.class);
                BinDocument d = bins.findById(id).orElseThrow(() -> ApiException.notFound("Benne introuvable."));
                if (req.code() != null) d.setCode(req.code().trim());
                if (req.capacityKg() != null) d.setCapacityKg(req.capacityKg());
                if (req.status() != null) d.setStatus(req.status());
                yield BinResponse.from(bins.save(d));
            }
            case "equipment" -> {
                EquipmentPatch req = parseAndValidate(body, EquipmentPatch.class);
                EquipmentDocument d = equipment.findById(id).orElseThrow(() -> ApiException.notFound("Matériel introuvable."));
                if (req.type() != null) d.setType(req.type());
                if (req.label() != null) d.setLabel(req.label().trim());
                if (req.quantity() != null) d.setQuantity(req.quantity());
                if (req.status() != null) d.setStatus(req.status());
                yield EquipmentResponse.from(equipment.save(d));
            }
            default -> throw ApiException.validation("kind", "Doit valoir workers, vehicles, bins ou equipment");
        };
    }

    private <T> T parseAndValidate(JsonNode body, Class<T> type) {
        final T dto;
        try {
            dto = mapper.treeToValue(body, type);
        } catch (Exception e) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "JSON mal formé ou valeur invalide.", null);
        }
        Set<ConstraintViolation<T>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            Map<String, String> errors = new LinkedHashMap<>();
            violations.forEach(v -> errors.putIfAbsent(v.getPropertyPath().toString(), v.getMessage()));
            throw new ApiException(ErrorCode.VALIDATION_ERROR, null, errors);
        }
        return dto;
    }
}
