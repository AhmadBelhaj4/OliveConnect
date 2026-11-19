package tn.oliveconnect.resource.dto;

import jakarta.validation.constraints.*;
import java.util.List;
import tn.oliveconnect.common.enums.EquipmentType;
import tn.oliveconnect.common.enums.ResourceStatus;
import tn.oliveconnect.common.enums.VehicleType;
import tn.oliveconnect.resource.document.*;

public final class ResourceDtos {
    private ResourceDtos() {}

    // --- WORKER ---
    public record WorkerRequest(
            @NotBlank(message = "Obligatoire") @Size(max = 100) String fullName,
            @NotBlank(message = "Obligatoire") @Size(max = 30) String phone,
            @NotNull(message = "Obligatoire") List<@NotBlank String> skills,
            ResourceStatus status) {}

    public record WorkerPatch(
            @Size(min = 1, max = 100) String fullName,
            @Size(min = 1, max = 30) String phone,
            List<@NotBlank String> skills,
            ResourceStatus status) {}

    public record WorkerResponse(String id, String fullName, String phone, List<String> skills, ResourceStatus status) {
        public static WorkerResponse from(WorkerDocument d) {
            return new WorkerResponse(d.getId(), d.getFullName(), d.getPhone(), d.getSkills(), d.getStatus());
        }
    }

    // --- VEHICLE ---
    public record VehicleRequest(
            @NotNull(message = "Obligatoire") VehicleType type,
            @NotBlank(message = "Obligatoire") @Size(max = 40) String registration,
            @NotNull(message = "Obligatoire") @Positive(message = "Doit être > 0") Integer capacityKg,
            ResourceStatus status) {}

    public record VehiclePatch(
            VehicleType type,
            @Size(min = 1, max = 40) String registration,
            @Positive(message = "Doit être > 0") Integer capacityKg,
            ResourceStatus status) {}

    public record VehicleResponse(String id, VehicleType type, String registration, Integer capacityKg, ResourceStatus status) {
        public static VehicleResponse from(VehicleDocument d) {
            return new VehicleResponse(d.getId(), d.getType(), d.getRegistration(), d.getCapacityKg(), d.getStatus());
        }
    }

    // --- BIN ---
    public record BinRequest(
            @NotBlank(message = "Obligatoire") @Size(max = 40) String code,
            @NotNull(message = "Obligatoire") @Positive(message = "Doit être > 0") Integer capacityKg,
            ResourceStatus status) {}

    public record BinPatch(
            @Size(min = 1, max = 40) String code,
            @Positive(message = "Doit être > 0") Integer capacityKg,
            ResourceStatus status) {}

    public record BinResponse(String id, String code, Integer capacityKg, ResourceStatus status) {
        public static BinResponse from(BinDocument d) {
            return new BinResponse(d.getId(), d.getCode(), d.getCapacityKg(), d.getStatus());
        }
    }

    // --- EQUIPMENT ---
    public record EquipmentRequest(
            @NotNull(message = "Obligatoire") EquipmentType type,
            @NotBlank(message = "Obligatoire") @Size(max = 100) String label,
            @NotNull(message = "Obligatoire") @Positive(message = "Doit être > 0") Integer quantity,
            ResourceStatus status) {}

    public record EquipmentPatch(
            EquipmentType type,
            @Size(min = 1, max = 100) String label,
            @Positive(message = "Doit être > 0") Integer quantity,
            ResourceStatus status) {}

    public record EquipmentResponse(String id, EquipmentType type, String label, Integer quantity, ResourceStatus status) {
        public static EquipmentResponse from(EquipmentDocument d) {
            return new EquipmentResponse(d.getId(), d.getType(), d.getLabel(), d.getQuantity(), d.getStatus());
        }
    }
}
