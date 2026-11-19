import type {
  HarvestStatus,
  AlertStatus,
  AlertPriority,
  TourStatus,
  MaturityStatus,
  QualityGrade,
} from '../../types/contract';

export interface StatusStyle {
  label: string;
  fg: string;
  bg: string;
  dot?: string;
}

export const harvestStatusStyles: Record<HarvestStatus, StatusStyle> = {
  SUBMITTED: { label: 'Submitted', fg: '#3d6fa8', bg: '#dbeafe' },
  APPROVED: { label: 'Approved', fg: '#2e7d32', bg: '#d1f0d3' },
  REJECTED: { label: 'Rejected', fg: '#b33a3a', bg: '#fee2e2' },
  SCHEDULED: { label: 'Scheduled', fg: '#b45309', bg: '#fef3c7' },
  IN_PROGRESS: {
    label: 'In progress',
    fg: '#b07f1f',
    bg: '#f7eccf',
    dot: '#b07f1f',
  },
  COMPLETED: { label: 'Completed', fg: '#1b5e20', bg: '#d1fae5' },
  CANCELLED: { label: 'Cancelled', fg: '#5e6650', bg: '#ecebe3' },
};

export const alertStatusStyles: Record<AlertStatus, StatusStyle> = {
  OPEN: {
    label: 'Open',
    fg: '#b33a3a',
    bg: '#fee2e2',
    dot: '#b33a3a',
  },
  ACKNOWLEDGED: {
    label: 'Acknowledged',
    fg: '#b45309',
    bg: '#fef3c7',
    dot: '#b45309',
  },
  RESOLVED: {
    label: 'Resolved',
    fg: '#2e7d32',
    bg: '#d1f0d3',
    dot: '#2e7d32',
  },
  DISMISSED: { label: 'Dismissed', fg: '#5e6650', bg: '#ecebe3' },
};

export const alertPriorityStyles: Record<AlertPriority, StatusStyle> = {
  LOW: { label: 'Low', fg: '#5e6650', bg: '#ecebe3' },
  MEDIUM: { label: 'Medium', fg: '#3d6fa8', bg: '#dbeafe' },
  HIGH: { label: 'High', fg: '#b33a3a', bg: '#fee2e2' },
};

export const tourStatusStyles: Record<TourStatus, StatusStyle> = {
  DRAFT: { label: 'Draft', fg: '#5e6650', bg: '#ecebe3' },
  PLANNED: { label: 'Planned', fg: '#b45309', bg: '#fef3c7' },
  IN_PROGRESS: {
    label: 'In progress',
    fg: '#b07f1f',
    bg: '#f7eccf',
    dot: '#b07f1f',
  },
  COMPLETED: { label: 'Completed', fg: '#1b5e20', bg: '#d1fae5' },
  CANCELLED: { label: 'Cancelled', fg: '#5e6650', bg: '#ecebe3' },
};

export const maturityStyles: Record<
  MaturityStatus,
  { label: string; color: string }
> = {
  UNKNOWN: { label: 'Unknown', color: '#8b917c' },
  IMMATURE: { label: 'Immature', color: '#a8be72' },
  APPROACHING: { label: 'Approaching', color: '#e0b856' },
  OPTIMAL: { label: 'Optimal', color: '#4a6b2a' },
  OVERRIPE: { label: 'Overripe', color: '#b33a3a' },
};

export const qualityGradeStyles: Record<QualityGrade, StatusStyle> = {
  A: { label: 'Grade A', fg: '#1b5e20', bg: '#d1fae5' },
  B: { label: 'Grade B', fg: '#b45309', bg: '#fef3c7' },
  C: { label: 'Grade C', fg: '#b33a3a', bg: '#fee2e2' },
};
