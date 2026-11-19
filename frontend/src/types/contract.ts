// Contrat TypeScript v1 — Miroir strict de docs/api-contract.md

export type Role =
  | "FARMER"
  | "COLLECTION_MANAGER"
  | "TEAM_LEADER"
  | "LOGISTICS_MANAGER"
  | "PRESS_MANAGER"
  | "ADMIN";

export type MaturityStatus =
  | "UNKNOWN"
  | "IMMATURE"
  | "APPROACHING"
  | "OPTIMAL"
  | "OVERRIPE";

export type HarvestType = "PLANNED" | "URGENT";

export type HarvestStatus =
  | "SUBMITTED"
  | "APPROVED"
  | "REJECTED"
  | "SCHEDULED"
  | "IN_PROGRESS"
  | "COMPLETED"
  | "CANCELLED";

export interface GeoLocation {
  latitude: number;
  longitude: number;
}

export interface UserResponse {
  id: string;
  email: string;
  fullName: string;
  phone?: string;
  role: Role;
  active: boolean;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
  userId: string;
  email: string;
  fullName: string;
  role: Role;
}

export interface OrchardResponse {
  id: string;
  farmerId: string;
  name: string;
  surfaceHectares: number;
  treeCount: number;
  oliveVariety: string;
  location: GeoLocation;
  maturityStatus: MaturityStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateOrchardRequest {
  name: string;
  surfaceHectares: number;
  treeCount: number;
  oliveVariety: string;
  location: GeoLocation;
  maturityStatus?: MaturityStatus;
}

export interface HarvestRequestResponse {
  id: string;
  orchardId: string;
  farmerId: string;
  type: HarvestType;
  estimatedQuantityKg: number;
  desiredStartDate: string;
  status: HarvestStatus;
  notes?: string;
  rejectionReason?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateHarvestRequest {
  orchardId: string;
  type: HarvestType;
  estimatedQuantityKg: number;
  desiredStartDate: string;
  notes?: string;
}

export interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  validationErrors?: Record<string, string>;
}
