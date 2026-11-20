import { http, HttpResponse } from 'msw';
import type {
  HarvestRequestCreateRequest,
  HarvestRequestResponse,
  LoginRequest,
  OrchardCreateRequest,
  OrchardMapFeatureCollection,
  OrchardResponse,
  Role,
  UserResponse,
} from '../types/contract';

const DEMO_USERS: Record<string, UserResponse> = {
  'farmer@example.test': {
    id: '6741a0c2e4b0a1b2c3d4e5f6',
    fullName: 'Example Farmer',
    email: 'farmer@example.test',
    role: 'FARMER',
    active: true,
    createdAt: '2026-11-17T10:00:00Z',
  },
  'collection@example.test': {
    id: '6741a0c2e4b0a1b2c3d4e501',
    fullName: 'Resp. Collecte',
    email: 'collection@example.test',
    role: 'COLLECTION_MANAGER',
    active: true,
    createdAt: '2026-11-17T10:00:00Z',
  },
  'leader@example.test': {
    id: '6741b0c2e4b0a1b2c3d4e502',
    fullName: 'Chef Équipe 01',
    email: 'leader@example.test',
    role: 'TEAM_LEADER',
    active: true,
    createdAt: '2026-11-17T10:00:00Z',
  },
  'logistics@example.test': {
    id: '6741a0c2e4b0a1b2c3d4e503',
    fullName: 'Resp. Logistique',
    email: 'logistics@example.test',
    role: 'LOGISTICS_MANAGER',
    active: true,
    createdAt: '2026-11-17T10:00:00Z',
  },
  'press@example.test': {
    id: '6741ab00e4b0a1b2c3d4e504',
    fullName: 'Resp. Pressoir',
    email: 'press@example.test',
    role: 'PRESS_MANAGER',
    active: true,
    createdAt: '2026-11-17T10:00:00Z',
  },
  'admin@example.test': {
    id: '6741a0c2e4b0a1b2c3d4e505',
    fullName: 'Administrateur',
    email: 'admin@example.test',
    role: 'ADMIN',
    active: true,
    createdAt: '2026-11-17T10:00:00Z',
  },
};

const orchards: OrchardResponse[] = [
  {
    id: '6741a1c2e4b0a1b2c3d4e5f6',
    farmerId: '6741a0c2e4b0a1b2c3d4e5f6',
    name: 'North Orchard',
    areaHectares: 4.2,
    oliveVariety: 'Chemlali',
    location: { type: 'Point', coordinates: [10.76, 34.73] },
    maturityStatus: 'OPTIMAL',
    publicVisibility: true,
    createdAt: '2026-11-17T10:00:00Z',
    updatedAt: '2026-11-17T10:00:00Z',
  },
];

const harvestRequests: HarvestRequestResponse[] = [
  {
    id: '6741a2c2e4b0a1b2c3d4e5f6',
    farmerId: '6741a0c2e4b0a1b2c3d4e5f6',
    orchardId: '6741a1c2e4b0a1b2c3d4e5f6',
    orchardName: 'North Orchard',
    type: 'URGENT',
    urgencyReason: 'Maturité optimale',
    status: 'SUBMITTED',
    requestedAt: '2026-11-17T10:30:00Z',
    preferredDate: '2026-11-20',
    estimatedQuantityKg: 2500,
    actualQuantityKg: null,
    reviewedBy: null,
    reviewNote: null,
    scheduledTourId: null,
    scheduledAt: null,
    completedAt: null,
    createdAt: '2026-11-17T10:30:00Z',
    updatedAt: '2026-11-17T10:30:00Z',
  },
];

let currentEmail = 'farmer@example.test';

export const handlers = [
  http.post('/api/v1/auth/login', async ({ request }) => {
    const body = (await request.json()) as LoginRequest;
    const user = DEMO_USERS[body.email.trim().toLowerCase()];
    if (!user || body.password !== 'Secret#123') {
      return HttpResponse.json(
        {
          timestamp: new Date().toISOString(),
          status: 401,
          code: 'UNAUTHENTICATED',
          message: 'Email ou mot de passe incorrect.',
          fieldErrors: {},
        },
        { status: 401 },
      );
    }
    currentEmail = user.email;
    return HttpResponse.json({
      accessToken: `mock-jwt-${user.role}`,
      tokenType: 'Bearer',
      expiresInSeconds: 7200,
      user,
    });
  }),

  http.get('/api/v1/users/me', ({ request }) => {
    const auth = request.headers.get('Authorization');
    if (!auth) {
      return HttpResponse.json(
        { timestamp: new Date().toISOString(), status: 401, code: 'UNAUTHENTICATED', message: 'Token manquant.', fieldErrors: {} },
        { status: 401 },
      );
    }
    return HttpResponse.json(DEMO_USERS[currentEmail] ?? DEMO_USERS['farmer@example.test']);
  }),

  http.get('/api/v1/orchards/mine', () =>
    HttpResponse.json({
      content: orchards,
      page: 0,
      size: 20,
      totalElements: orchards.length,
      totalPages: 1,
    }),
  ),

  http.post('/api/v1/orchards', async ({ request }) => {
    const body = (await request.json()) as OrchardCreateRequest;
    if (body.areaHectares <= 0) {
      return HttpResponse.json(
        {
          timestamp: new Date().toISOString(),
          status: 400,
          code: 'VALIDATION_ERROR',
          message: 'Requête invalide.',
          fieldErrors: { areaHectares: 'Doit être > 0' },
        },
        { status: 400 },
      );
    }
    const created: OrchardResponse = {
      id: `6741a1c2e4b0a1b2c3d4e${String(orchards.length + 100)}`,
      farmerId: '6741a0c2e4b0a1b2c3d4e5f6',
      name: body.name,
      areaHectares: body.areaHectares,
      oliveVariety: body.oliveVariety,
      location: body.location,
      maturityStatus: 'UNKNOWN',
      publicVisibility: body.publicVisibility,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };
    orchards.unshift(created);
    return HttpResponse.json(created, { status: 201 });
  }),

  http.get('/api/v1/harvest-requests/mine', () =>
    HttpResponse.json({
      content: harvestRequests,
      page: 0,
      size: 20,
      totalElements: harvestRequests.length,
      totalPages: 1,
    }),
  ),

  http.post('/api/v1/harvest-requests', async ({ request }) => {
    const body = (await request.json()) as HarvestRequestCreateRequest;
    const fieldErrors: Record<string, string> = {};
    if (body.estimatedQuantityKg <= 0) fieldErrors.estimatedQuantityKg = 'Doit être > 0';
    if (body.type === 'URGENT' && (!body.urgencyReason || !body.urgencyReason.trim())) {
      fieldErrors.urgencyReason = 'Obligatoire pour une demande urgente';
    }
    if (Object.keys(fieldErrors).length > 0) {
      return HttpResponse.json(
        { timestamp: new Date().toISOString(), status: 400, code: 'VALIDATION_ERROR', message: 'Requête invalide.', fieldErrors },
        { status: 400 },
      );
    }
    const orchard = orchards.find((o) => o.id === body.orchardId);
    const created: HarvestRequestResponse = {
      id: `6741a2c2e4b0a1b2c3d4e${String(harvestRequests.length + 200)}`,
      farmerId: '6741a0c2e4b0a1b2c3d4e5f6',
      orchardId: body.orchardId,
      orchardName: orchard?.name ?? 'North Orchard',
      type: body.type,
      urgencyReason: body.type === 'URGENT' ? body.urgencyReason : null,
      status: 'SUBMITTED',
      requestedAt: new Date().toISOString(),
      preferredDate: body.preferredDate,
      estimatedQuantityKg: body.estimatedQuantityKg,
      actualQuantityKg: null,
      reviewedBy: null,
      reviewNote: null,
      scheduledTourId: null,
      scheduledAt: null,
      completedAt: null,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };
    harvestRequests.unshift(created);
    return HttpResponse.json(created, { status: 201 });
  }),

  http.get('/api/v1/orchards/map', ({ request }) => {
    const url = new URL(request.url);
    if (!url.searchParams.get('bbox')) {
      return HttpResponse.json(
        { timestamp: new Date().toISOString(), status: 400, code: 'VALIDATION_ERROR', message: 'bbox obligatoire', fieldErrors: { bbox: 'Obligatoire' } },
        { status: 400 },
      );
    }
    const isAuth = Boolean(request.headers.get('Authorization'));
    const fc: OrchardMapFeatureCollection = {
      type: 'FeatureCollection',
      truncated: false,
      features: orchards.map((o) => ({
        type: 'Feature',
        geometry: o.location,
        properties: {
          orchardId: o.id,
          name: isAuth ? o.name : null,
          oliveVariety: o.oliveVariety,
          areaHectares: isAuth ? o.areaHectares : null,
          farmerId: isAuth ? o.farmerId : null,
          maturityStatus: o.maturityStatus,
          harvestStatus: 'SCHEDULED',
        },
      })),
    };
    return HttpResponse.json(fc);
  }),

  http.get('/api/v1/harvest-requests', ({ request }) => {
    const url = new URL(request.url);
    const status = url.searchParams.get('status');
    const filtered = status ? harvestRequests.filter((r) => r.status === status) : harvestRequests;
    return HttpResponse.json({
      content: filtered,
      page: 0,
      size: 20,
      totalElements: filtered.length,
      totalPages: 1,
    });
  }),

  http.post('/api/v1/harvest-requests/:id/review', async ({ params, request }) => {
    const { id } = params;
    const body = (await request.json()) as { decision: 'APPROVE' | 'REJECT'; reason?: string };
    const req = harvestRequests.find((r) => r.id === id);
    if (!req) return HttpResponse.json({ message: 'Demande introuvable' }, { status: 404 });
    req.status = body.decision === 'APPROVE' ? 'APPROVED' : 'REJECTED';
    req.reviewNote = body.reason || null;
    return HttpResponse.json(req);
  }),

  http.get('/api/v1/resources/availability', ({ request }) => {
    const url = new URL(request.url);
    const date = url.searchParams.get('date') || '2026-11-20';
    return HttpResponse.json({
      date,
      workers: [
        { id: '6741w1c2e4b0a1b2c3d4e001', fullName: 'Ali Ben Salah', phone: '21698111222', skills: ['PICKING'], status: 'AVAILABLE' },
        { id: '6741w2c2e4b0a1b2c3d4e002', fullName: 'Mohamed Trabelsi', phone: '21698333444', skills: ['PICKING'], status: 'AVAILABLE' },
      ],
      vehicles: [
        { id: '6741v1c2e4b0a1b2c3d4e001', type: 'TRACTOR', registration: '120 TUN 4567', capacityKg: 3500, status: 'AVAILABLE' },
      ],
      bins: [
        { id: '6741b1c2e4b0a1b2c3d4e001', code: 'BIN-101', capacityKg: 500, status: 'AVAILABLE' },
        { id: '6741b2c2e4b0a1b2c3d4e002', code: 'BIN-102', capacityKg: 500, status: 'AVAILABLE' },
      ],
      equipment: [],
    });
  }),

  http.get('/api/v1/tours', () =>
    HttpResponse.json({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    }),
  ),

  http.post('/api/v1/tours', async ({ request }) => {
    const body = (await request.json()) as { tourDate: string };
    return HttpResponse.json(
      {
        id: '6741t1c2e4b0a1b2c3d4e001',
        tourDate: body.tourDate,
        status: 'DRAFT',
        teamLeaderId: null,
        vehicleIds: [],
        workerIds: [],
        binIds: [],
        stops: [],
        startedAt: null,
        completedAt: null,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      },
      { status: 201 },
    );
  }),

  http.post('/api/v1/tours/:id/plan', async ({ params, request }) => {
    const { id } = params;
    const body = (await request.json()) as any;
    return HttpResponse.json({
      id,
      tourDate: '2026-11-20',
      status: 'PLANNED',
      teamLeaderId: body.teamLeaderId,
      vehicleIds: body.vehicleIds,
      workerIds: body.workerIds,
      binIds: body.binIds,
      stops: (body.stops || []).map((s: any, idx: number) => ({
        stopId: `stop-${idx}`,
        sequence: s.sequence,
        harvestRequestId: s.harvestRequestId,
        orchardId: '6741a1c2e4b0a1b2c3d4e5f6',
        orchardName: 'North Orchard',
        plannedQuantityKg: 2500,
        actualQuantityKg: null,
        status: 'PLANNED',
      })),
      startedAt: null,
      completedAt: null,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    });
  }),

  http.post('/api/v1/tours/:id/start', async ({ params }) => {
    return HttpResponse.json({ id: params.id, status: 'IN_PROGRESS' });
  }),

  http.post('/api/v1/tours/:id/stops/:stopId/complete', async ({ params }) => {
    return HttpResponse.json({ id: params.id, status: 'IN_PROGRESS' });
  }),

  http.post('/api/v1/tours/:id/complete', async ({ params }) => {
    return HttpResponse.json({ id: params.id, status: 'COMPLETED' });
  }),

  http.get('/api/v1/alerts', () =>
    HttpResponse.json({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    }),
  ),

  http.post('/api/v1/alerts', async ({ request }) => {
    const body = (await request.json()) as any;
    return HttpResponse.json(
      {
        id: '6741a9c2e4b0a1b2c3d4e001',
        farmerId: '6741a0c2e4b0a1b2c3d4e5f6',
        orchardId: body.orchardId,
        type: body.type,
        description: body.description,
        location: body.location,
        priority: body.priority,
        status: 'OPEN',
        createdAt: new Date().toISOString(),
        acknowledgedAt: null,
        resolvedAt: null,
        resolutionNote: null,
      },
      { status: 201 },
    );
  }),

  http.get('/api/v1/notifications', () =>
    HttpResponse.json({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    }),
  ),

  http.post('/api/v1/notifications/:id/read', ({ params }) =>
    HttpResponse.json({
      id: params.id,
      readAt: new Date().toISOString(),
    }),
  ),

  http.get('/api/v1/presses', () =>
    HttpResponse.json({
      content: [
        {
          id: '6741p1c2e4b0a1b2c3d4e001',
          name: 'Pressoir Central Sfax',
          location: { type: 'Point', coordinates: [10.75, 34.74] },
          dailyCapacityKg: 50000,
          availableCapacityKg: 35000,
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    }),
  ),

  http.patch('/api/v1/presses/:id/capacity', async ({ params, request }) => {
    const body = (await request.json()) as { availableCapacityKg: number };
    return HttpResponse.json({
      id: params.id,
      name: 'Pressoir Central Sfax',
      location: { type: 'Point', coordinates: [10.75, 34.74] },
      dailyCapacityKg: 50000,
      availableCapacityKg: body.availableCapacityKg,
    });
  }),

  http.get('/api/v1/press-receptions', () =>
    HttpResponse.json({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    }),
  ),

  http.post('/api/v1/press-receptions', async ({ request }) => {
    const body = (await request.json()) as any;
    return HttpResponse.json(
      {
        id: '6741r1c2e4b0a1b2c3d4e001',
        tourId: body.tourId,
        binId: body.binId,
        pressId: body.pressId,
        receivedAt: new Date().toISOString(),
        weightKg: body.weightKg,
        qualityGrade: body.qualityGrade,
        notes: body.notes,
        recordedBy: '6741ab00e4b0a1b2c3d4e504',
      },
      { status: 201 },
    );
  }),

  http.get('/api/v1/resources/vehicles', () =>
    HttpResponse.json({
      content: [
        { id: '6741v1c2e4b0a1b2c3d4e001', type: 'TRACTOR', registration: '120 TUN 4567', capacityKg: 3500, status: 'AVAILABLE' },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    }),
  ),

  // --- S3 Endpoints: Dashboard & Admin ---
  http.get('/api/v1/dashboard/summary', () =>
    HttpResponse.json({
      from: '2026-11-01',
      to: '2026-11-30',
      totalHarvestedKg: 4200.0,
      totalReceivedKg: 3800.0,
      completedTours: 3,
      openAlerts: 1,
      requestsByStatus: {
        SUBMITTED: 2,
        APPROVED: 1,
        REJECTED: 0,
        SCHEDULED: 1,
        IN_PROGRESS: 1,
        COMPLETED: 3,
        CANCELLED: 0,
      },
    }),
  ),

  http.get('/api/v1/dashboard/yield-by-orchard', () =>
    HttpResponse.json({
      from: '2026-11-01',
      to: '2026-11-30',
      items: [
        { orchardId: '6741a100e4b0a1b2c3d4e501', orchardName: 'Verger Sfax Nord', totalHarvestedKg: 2500.0, collectionCount: 2 },
        { orchardId: '6741a100e4b0a1b2c3d4e502', orchardName: 'Verger Thyna Sud', totalHarvestedKg: 1700.0, collectionCount: 1 },
      ],
    }),
  ),

  http.get('/api/v1/dashboard/collection-delays', () =>
    HttpResponse.json({
      from: '2026-11-01',
      to: '2026-11-30',
      sampleSize: 3,
      avgSubmittedToScheduledHours: 18.5,
      avgScheduledToCompletedHours: 22.0,
    }),
  ),

  http.get('/api/v1/dashboard/bin-fill-rates', () =>
    HttpResponse.json({
      from: '2026-11-01',
      to: '2026-11-30',
      items: [
        { binId: 'bin-01', binCode: 'BIN-01', capacityKg: 500, receptionCount: 2, avgFillRate: 0.85 },
        { binId: 'bin-02', binCode: 'BIN-02', capacityKg: 600, receptionCount: 1, avgFillRate: 0.72 },
      ],
    }),
  ),

  http.get('/api/v1/admin/users', () =>
    HttpResponse.json({
      content: Object.values(DEMO_USERS),
      page: 0,
      size: 20,
      totalElements: Object.values(DEMO_USERS).length,
      totalPages: 1,
    }),
  ),

  http.patch('/api/v1/admin/users/:id/role', async ({ params, request }) => {
    const body = (await request.json()) as { role: Role };
    const user = Object.values(DEMO_USERS).find((u) => u.id === params.id) || DEMO_USERS['farmer@example.test'];
    return HttpResponse.json({ ...user, role: body.role });
  }),

  http.patch('/api/v1/admin/users/:id/active', async ({ params, request }) => {
    const body = (await request.json()) as { active: boolean };
    const user = Object.values(DEMO_USERS).find((u) => u.id === params.id) || DEMO_USERS['farmer@example.test'];
    return HttpResponse.json({ ...user, active: body.active });
  }),

  http.get('/api/v1/admin/imports', () =>
    HttpResponse.json({
      content: [
        {
          id: 'imp-001',
          type: 'ORCHARDS',
          sourceFile: 'orchards_sfax.csv',
          status: 'COMPLETED',
          startedAt: new Date(Date.now() - 3600000).toISOString(),
          finishedAt: new Date().toISOString(),
          readCount: 10,
          insertedCount: 10,
          updatedCount: 0,
          rejectedCount: 0,
          errors: [],
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    }),
  ),

  http.post('/api/v1/admin/imports', () =>
    HttpResponse.json(
      {
        id: 'imp-' + Date.now(),
        type: 'ORCHARDS',
        sourceFile: 'uploaded.csv',
        status: 'COMPLETED',
        startedAt: new Date().toISOString(),
        finishedAt: new Date().toISOString(),
        readCount: 2,
        insertedCount: 2,
        updatedCount: 0,
        rejectedCount: 0,
        errors: [],
      },
      { status: 201 },
    ),
  ),
];

export const SEEDED_ACCOUNTS: { label: string; email: string; role: Role }[] = Object.values(DEMO_USERS).map((u) => ({
  label: `${u.role} (${u.email})`,
  email: u.email,
  role: u.role,
}));
