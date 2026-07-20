import client from './client'
import type { User, CreateUserRequest } from './types'

export const usersApi = {
  list(clinicId: number) {
    return client.get<User[]>('/users', { params: { clinicId } })
  },
  create(data: CreateUserRequest) {
    return client.post<User>('/users', data)
  },
  activate(userId: number, data: { clinicId: number }) {
    return client.patch<User>(`/users/${userId}/activate`, data)
  },
  deactivate(userId: number, data: { clinicId: number }) {
    return client.patch<User>(`/users/${userId}/deactivate`, data)
  },
}
