import client from './client'
import type { AuditLogEntry, AuditFilters } from './types'

export const auditApi = {
  list(clinicId: number, filters: AuditFilters = {}) {
    return client.get<AuditLogEntry[]>('/audit', { params: { clinicId, ...filters } })
  },
}
