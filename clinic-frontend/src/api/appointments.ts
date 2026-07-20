import client from './client'
import type { Appointment, BookAppointmentRequest, RescheduleRequest, UpdateAppointmentStatusRequest } from './types'

export const appointmentsApi = {
  list(clinicId: number) {
    return client.get<Appointment[]>('/appointments', { params: { clinicId } })
  },
  get(appointmentId: number, clinicId: number) {
    return client.get<Appointment>(`/appointments/${appointmentId}`, { params: { clinicId } })
  },
  listByCustomer(customerId: number, clinicId: number) {
    return client.get<Appointment[]>(`/appointments/customer/${customerId}`, { params: { clinicId } })
  },
  listByDoctor(doctorId: number, clinicId: number) {
    return client.get<Appointment[]>(`/appointments/doctor/${doctorId}`, { params: { clinicId } })
  },
  book(data: BookAppointmentRequest) {
    return client.post<Appointment>('/appointments', data, { params: { clinicId: data.clinicId } })
  },
  cancel(appointmentId: number, clinicId: number) {
    return client.put<Appointment>(`/appointments/${appointmentId}/cancel`, null, { params: { clinicId } })
  },
  reschedule(appointmentId: number, clinicId: number, data: RescheduleRequest) {
    return client.put<Appointment>(`/appointments/${appointmentId}/reschedule`, data, { params: { clinicId } })
  },
  updateStatus(appointmentId: number, clinicId: number, data: UpdateAppointmentStatusRequest) {
    return client.put<Appointment>(`/appointments/${appointmentId}/status`, data, { params: { clinicId } })
  },
  remove(appointmentId: number, clinicId: number) {
    return client.delete(`/appointments/${appointmentId}`, { params: { clinicId } })
  },
}
