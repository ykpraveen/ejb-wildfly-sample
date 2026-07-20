import client from './client'
import type { Doctor, CreateDoctorRequest, UpdateDoctorRequest } from './types'

export const doctorsApi = {
  list(clinicId: number) {
    return client.get<Doctor[]>('/doctors', { params: { clinicId } })
  },
  get(doctorId: number, clinicId: number) {
    return client.get<Doctor>(`/doctors/${doctorId}`, { params: { clinicId } })
  },
  create(data: CreateDoctorRequest) {
    return client.post<Doctor>('/doctors', data)
  },
  update(doctorId: number, clinicId: number, data: UpdateDoctorRequest) {
    return client.put<Doctor>(`/doctors/${doctorId}`, data, { params: { clinicId } })
  },
  remove(doctorId: number, clinicId: number) {
    return client.delete(`/doctors/${doctorId}`, { params: { clinicId } })
  },
}
