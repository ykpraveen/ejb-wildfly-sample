import client from './client'
import type { Customer, CreateCustomerRequest, UpdateCustomerRequest } from './types'

export const customersApi = {
  list(clinicId: number) {
    return client.get<Customer[]>('/customers', { params: { clinicId } })
  },
  get(customerId: number, clinicId: number) {
    return client.get<Customer>(`/customers/${customerId}`, { params: { clinicId } })
  },
  create(data: CreateCustomerRequest) {
    return client.post<Customer>('/customers', data)
  },
  update(customerId: number, clinicId: number, data: UpdateCustomerRequest) {
    return client.put<Customer>(`/customers/${customerId}`, data, { params: { clinicId } })
  },
  remove(customerId: number, clinicId: number) {
    return client.delete(`/customers/${customerId}`, { params: { clinicId } })
  },
}
