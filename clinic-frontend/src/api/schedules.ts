import client from './client'
import type { DoctorSchedule, CreateScheduleRequest, UpdateScheduleRequest } from './types'

export const schedulesApi = {
  list(doctorId: number, clinicId: number) {
    return client.get<DoctorSchedule[]>(`/doctors/${doctorId}/schedules`, { params: { clinicId } })
  },
  get(doctorId: number, scheduleId: number, clinicId: number) {
    return client.get<DoctorSchedule>(`/doctors/${doctorId}/schedules/${scheduleId}`, { params: { clinicId } })
  },
  create(doctorId: number, data: CreateScheduleRequest) {
    return client.post<DoctorSchedule>(`/doctors/${doctorId}/schedules`, data)
  },
  update(doctorId: number, scheduleId: number, clinicId: number, data: UpdateScheduleRequest) {
    return client.put<DoctorSchedule>(`/doctors/${doctorId}/schedules/${scheduleId}`, data, { params: { clinicId } })
  },
  remove(doctorId: number, scheduleId: number, clinicId: number) {
    return client.delete(`/doctors/${doctorId}/schedules/${scheduleId}`, { params: { clinicId } })
  },
}
