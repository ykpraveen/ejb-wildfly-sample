import client from './client'
import type { StartBookingRequest, BookingSummary } from './types'

export const bookingsApi = {
  start(data: StartBookingRequest) {
    return client.post<{ sessionId: string }>('/bookings', data)
  },
  getSummary(sessionId: string) {
    return client.get<BookingSummary>(`/bookings/${sessionId}`)
  },
  selectDoctor(sessionId: string, doctorId: number) {
    return client.put(`/bookings/${sessionId}/doctor`, { doctorId })
  },
  selectSchedule(sessionId: string, scheduleId: number) {
    return client.put(`/bookings/${sessionId}/schedule`, { scheduleId })
  },
  selectTime(sessionId: string, appointmentTime: string) {
    return client.put(`/bookings/${sessionId}/time`, { appointmentTime })
  },
  addNotes(sessionId: string, notes: string) {
    return client.put(`/bookings/${sessionId}/notes`, { notes })
  },
  confirm(sessionId: string) {
    return client.post(`/bookings/${sessionId}/confirm`)
  },
  cancel(sessionId: string) {
    return client.delete(`/bookings/${sessionId}`)
  },
}
