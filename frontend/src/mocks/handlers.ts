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
];

export const SEEDED_ACCOUNTS: { label: string; email: string; role: Role }[] = Object.values(DEMO_USERS).map((u) => ({
  label: `${u.role} (${u.email})`,
  email: u.email,
  role: u.role,
}));
