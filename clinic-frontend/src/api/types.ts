// Auth
export interface LoginRequest { clinicId: number; username: string; password: string }
export interface LoginResponse { accessToken: string; tokenType: string; clinicId: number; username: string; roles: string[] }

// Users
export interface User { id: number; clinicId: number; username: string; roles: string[]; active: boolean; deleted: boolean; createdAt: string; updatedAt: string }
export interface CreateUserRequest { clinicId: number; username: string; password: string; roles: string[] }
export interface ActivateUserRequest { clinicId: number }

// Customers
export interface Customer { id: number; clinicId: number; fullName: string; username: string; email: string; phone: string; deleted: boolean; createdAt: string; updatedAt: string }
export interface CreateCustomerRequest { clinicId: number; fullName: string; username: string; email: string; phone: string }
export interface UpdateCustomerRequest { fullName?: string; email?: string; phone?: string }

// Doctors
export interface Doctor { id: number; clinicId: number; fullName: string; username: string; specialty: string; active: boolean; deleted: boolean; slotMinutes: number; createdAt: string; updatedAt: string }
export interface CreateDoctorRequest { clinicId: number; fullName: string; username: string; specialty: string; active: boolean; slotMinutes: number }
export interface UpdateDoctorRequest { fullName?: string; specialty?: string; active?: boolean; slotMinutes?: number }

// Schedules
export interface DoctorSchedule { id: number; clinicId: number; doctorId: number; availableDate: string; startTime: string; endTime: string; capacity: number; deleted: boolean; createdAt: string; updatedAt: string }
export interface CreateScheduleRequest { clinicId: number; doctorId: number; availableDate: string; startTime: string; endTime: string; capacity: number }
export interface UpdateScheduleRequest { startTime?: string; endTime?: string; capacity?: number }

// Appointments
export type AppointmentStatus = 'BOOKED' | 'CANCELLED' | 'COMPLETED'
export interface Appointment { id: number; clinicId: number; customerId: number; doctorId: number; scheduleId: number; appointmentDate: string; appointmentTime: string; status: AppointmentStatus; notes: string; createdBy: string; deleted: boolean; rescheduleCount: number; createdAt: string; updatedAt: string }
export interface BookAppointmentRequest { clinicId: number; customerId: number; doctorId: number; scheduleId: number; appointmentTime: string; notes?: string }
export interface RescheduleRequest { newTime: string }
export interface UpdateAppointmentStatusRequest { status: string }

// Booking Wizard
export interface StartBookingRequest { clinicId: number; customerId: number }
export interface SelectDoctorRequest { doctorId: number }
export interface SelectScheduleRequest { scheduleId: number }
export interface SelectTimeRequest { appointmentTime: string }
export interface AddNotesRequest { notes: string }
export interface BookingSummary { sessionId: string; clinicId: number; customerId: number; doctorId: number; doctorName: string; scheduleId: number; scheduleDate: string; scheduleStartTime: string; scheduleEndTime: string; selectedTime: string; notes: string; status: string }

// Audit
export interface AuditLogEntry { id: number; clinicId: number; actor: string; action: string; entityType: string; entityId: number; details: string; correlationId: string; createdAt: string }
export interface AuditFilters { entityType?: string; entityId?: number; actor?: string; limit?: number }

// Error
export interface ApiError { code: string; message: string; correlationId: string }
