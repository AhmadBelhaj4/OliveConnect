package tn.oliveconnect.harvest.dto;

import jakarta.validation.constraints.Size;

public record CancelRequest(@Size(max = 500, message = "500 caractères maximum") String reason) {}
