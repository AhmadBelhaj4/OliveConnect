// ============================================================================
// CONTRAT API v1 FIGÉ (sections 5.2 à 5.5 du plan) — NE RIEN RENOMMER
// ============================================================================

export type Role =
  | 'FARMER'
  | 'COLLECTION_MANAGER'
  | 'TEAM_LEADER'
  | 'LOGISTICS_MANAGER'
  | 'PRESS_MANAGER'
  | 'ADMIN';

export type MaturityStatus = 'UNKNOWN' | 'IMMATURE' | 'APPROACHING' | 'OPTIMAL' | 'OVERRIPE';
export type HarvestType = 'PLANNED' | 'URGENT';
export type HarvestStatus =
  | 'SUBMITTED'
  | 'APPROVED'
  | 'REJECTED'
  | 'SCHEDULED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED';

export type AlertType = 'MATURITY' | 'PEST' | 'RAIN' | 'OTHER';
export type AlertPriority = 'LOW' | 'MEDIUM' | 'HIGH';
export type AlertStatus = 'OPEN' | 'ACKNOWLEDGED' | 'RESOLVED' | 'DISMISSED';
export type TourStatus = 'DRAFT' | 'PLANNED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type StopStatus = 'PLANNED' | 'COMPLETED';
export type ResourceStatus = 'AVAILABLE' | 'IN_USE' | 'MAINTENANCE';
export type VehicleType = 'TRACTOR' | 'TRUCK';
export type EquipmentType = 'NET' | 'VIBRATOR' | 'OTHER';
export type QualityGrade = 'A' | 'B' | 'C';

export type NotificationType =
  | 'REQUEST_APPROVED'
  | 'REQUEST_REJECTED'
  | 'TOUR_SCHEDULED'
  | 'TOUR_CHANGED'
  | 'COLLECTION_STARTED'
  | 'COLLECTION_COMPLETED'
  | 'ALERT_ACKNOWLEDGED'
  | 'ALERT_RESOLVED'
  | 'ALERT_DISMISSED';

export type EntityType = 'HARVEST_REQUEST' | 'TOUR' | 'ALERT';

export type ImportType =
  | 'ORCHARDS'
  | 'WORKERS'
  | 'VEHICLES'
  | 'PRESSES'
  | 'MATURITY_READINGS'
  | 'WEATHER_OBSERVATIONS'
  | 'PRESS_CAPACITY';

export type ImportStatus = 'RUNNING' | 'COMPLETED' | 'PARTIAL' | 'FAILED';

export type ErrorCode =
  | 'VALIDATION_ERROR'
  | 'UNAUTHENTICATED'
  | 'ACCESS_DENIED'
  | 'NOT_FOUND'
  | 'INVALID_STATE_TRANSITION'
  | 'RESOURCE_CONFLICT'
  | 'DUPLICATE_RESOURCE'
  | 'RATE_LIMITED'
  | 'INTERNAL_ERROR';

export interface ApiError {
  timestamp: string;
  status: number;
  code: ErrorCode;
  message: string;
  fieldErrors: Record<string, string>;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** GeoJSON Point : coordinates = [longitude, latitude] (Sfax = [10.76, 34.73]) */
export interface GeoPoint {
  type: 'Point';
  coordinates: [number, number];
}

export interface UserResponse {
  id: string;
  fullName: string;
  email: string;
  role: Role;
  active: boolean;
  createdAt: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
  phone?: string | null;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: 'Bearer';
  expiresInSeconds: number;
  user: UserResponse;
}

export interface OrchardCreateRequest {
  name: string;
  areaHectares: number;
  oliveVariety: string;
  location: GeoPoint;
  publicVisibility: boolean;
}

export interface OrchardPatchRequest {
  name?: string;
  areaHectares?: number;
  oliveVariety?: string;
  location?: GeoPoint;
  publicVisibility?: boolean;
  maturityStatus?: MaturityStatus;
}

export interface OrchardResponse {
  id: string;
  farmerId: string;
  name: string;
  areaHectares: number;
  oliveVariety: string;
  location: GeoPoint;
  maturityStatus: MaturityStatus;
  publicVisibility: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface HarvestRequestCreateRequest {
  orchardId: string;
  type: HarvestType;
  preferredDate: string; // YYYY-MM-DD
  estimatedQuantityKg: number;
  urgencyReason: string | null;
}

export interface HarvestRequestReviewRequest {
  decision: 'APPROVE' | 'REJECT';
  reason: string | null;
}

export interface HarvestRequestCancelRequest {
  reason: string | null;
}

export interface HarvestRequestResponse {
  id: string;
  farmerId: string;
  orchardId: string;
  orchardName: string;
  type: HarvestType;
  urgencyReason: string | null;
  status: HarvestStatus;
  requestedAt: string;
  preferredDate: string;
  estimatedQuantityKg: number;
  actualQuantityKg: number | null;
  reviewedBy: string | null;
  reviewNote: string | null;
  scheduledTourId: string | null;
  scheduledAt: string | null;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface FieldAlertCreateRequest {
  orchardId: string;
  type: AlertType;
  description: string;
  location: GeoPoint;
  priority: AlertPriority;
}

export interface FieldAlertResponse {
  id: string;
  farmerId: string;
  orchardId: string;
  type: AlertType;
  description: string;
  location: GeoPoint;
  priority: AlertPriority;
  status: AlertStatus;
  createdAt: string;
  acknowledgedAt: string | null;
  resolvedAt: string | null;
  resolutionNote: string | null;
}

export interface TourPlanStopRequest {
  harvestRequestId: string;
  sequence: number;
}

export interface TourPlanRequest {
  teamLeaderId: string;
  vehicleIds: string[];
  workerIds: string[];
  binIds: string[];
  stops: TourPlanStopRequest[];
}

export interface TourStopResponse {
  stopId: string;
  sequence: number;
  harvestRequestId: string;
  orchardId: string;
  orchardName: string;
  plannedQuantityKg: number;
  actualQuantityKg: number | null;
  status: StopStatus;
}

export interface TourResponse {
  id: string;
  tourDate: string;
  status: TourStatus;
  teamLeaderId: string | null;
  vehicleIds: string[];
  workerIds: string[];
  binIds: string[];
  stops: TourStopResponse[];
  startedAt: string | null;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PressReceptionCreateRequest {
  tourId: string;
  binId: string;
  pressId: string;
  weightKg: number;
  qualityGrade: QualityGrade;
  notes: string | null;
  receivedAt: string | null;
}

export interface PressReceptionResponse {
  id: string;
  tourId: string;
  binId: string;
  pressId: string;
  receivedAt: string;
  weightKg: number;
  qualityGrade: QualityGrade;
  notes: string | null;
  recordedBy: string;
}

export interface WorkerResponse {
  id: string;
  fullName: string;
  phone: string;
  skills: string[];
  status: ResourceStatus;
}

export interface VehicleResponse {
  id: string;
  type: VehicleType;
  registration: string;
  capacityKg: number;
  status: ResourceStatus;
}

export interface BinResponse {
  id: string;
  code: string;
  capacityKg: number;
  status: ResourceStatus;
}

export interface EquipmentResponse {
  id: string;
  type: EquipmentType;
  label: string;
  quantity: number;
  status: ResourceStatus;
}

export interface PressResponse {
  id: string;
  name: string;
  location: GeoPoint;
  dailyCapacityKg: number;
  availableCapacityKg: number;
}

export interface ResourceAvailabilityResponse {
  date: string;
  workers: WorkerResponse[];
  vehicles: VehicleResponse[];
  bins: BinResponse[];
  equipment: EquipmentResponse[];
}

export interface NotificationResponse {
  id: string;
  type: NotificationType;
  message: string;
  entityType: EntityType;
  entityId: string;
  readAt: string | null;
  createdAt: string;
}

export interface OrchardMapProperties {
  orchardId: string;
  name: string | null;
  oliveVariety: string;
  areaHectares: number | null;
  farmerId: string | null;
  maturityStatus: MaturityStatus;
  harvestStatus: HarvestStatus | null;
}

export interface OrchardMapFeature {
  type: 'Feature';
  geometry: GeoPoint;
  properties: OrchardMapProperties;
}

export interface OrchardMapFeatureCollection {
  type: 'FeatureCollection';
  truncated: boolean;
  features: OrchardMapFeature[];
}

export interface DashboardSummaryResponse {
  from: string;
  to: string;
  totalHarvestedKg: number;
  totalReceivedKg: number;
  completedTours: number;
  openAlerts: number;
  requestsByStatus: Record<HarvestStatus, number>;
}

export interface DashboardYieldByOrchardResponse {
  from: string;
  to: string;
  items: {
    orchardId: string;
    orchardName: string;
    totalHarvestedKg: number;
    collectionCount: number;
  }[];
}

export interface DashboardCollectionDelaysResponse {
  from: string;
  to: string;
  sampleSize: number;
  avgSubmittedToScheduledHours: number | null;
  avgScheduledToCompletedHours: number | null;
}

export interface DashboardBinFillRatesResponse {
  from: string;
  to: string;
  items: {
    binId: string;
    binCode: string;
    capacityKg: number;
    receptionCount: number;
    avgFillRate: number | null;
  }[];
}

export interface ImportJobError {
  row: number;
  field: string;
  message: string;
}

export interface ImportJobResponse {
  id: string;
  type: ImportType;
  sourceFile: string;
  status: ImportStatus;
  startedAt: string;
  finishedAt: string | null;
  readCount: number;
  insertedCount: number;
  updatedCount: number;
  rejectedCount: number;
  errors: ImportJobError[];
}

// --- Corps de requête §5.5 utilisés en S2 (ajout de types, pas de changement de contrat) ---
export interface HarvestRequestPatchRequest {
  type?: HarvestType;
  preferredDate?: string;          // YYYY-MM-DD
  estimatedQuantityKg?: number;
  urgencyReason?: string | null;
}
export interface TourCreateRequest { tourDate: string; }
export interface StopCompleteRequest { actualQuantityKg: number; note: string | null; }
export interface PressCapacityRequest { availableCapacityKg: number; }
export interface AlertResolveRequest { note: string; }
export interface AlertDismissRequest { reason: string; }

