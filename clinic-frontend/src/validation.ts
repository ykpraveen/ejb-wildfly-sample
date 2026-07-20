import { z } from 'zod'

export const usernameSchema = z.string().min(3).max(50)
export const passwordSchema = z.string().min(8).max(100)
export const fullNameSchema = z.string().min(2).max(120)
export const emailSchema = z.string().email().max(120)
export const phoneSchema = z.string().max(30).optional().or(z.literal(''))
export const specialtySchema = z.string().min(1).max(100)
export const positiveInt = z.coerce.number().int().positive()
export const timeRegex = z.string().regex(/^\d{2}:\d{2}$/)
export const rolesSchema = z.array(z.string()).min(1)

export const loginSchema = z.object({
  clinicId: positiveInt,
  username: usernameSchema,
  password: z.string().min(6),
})

export const createUserSchema = z.object({
  username: usernameSchema,
  password: passwordSchema,
  roles: rolesSchema,
})

export const createCustomerSchema = z.object({
  fullName: fullNameSchema,
  username: usernameSchema,
  email: emailSchema,
  phone: phoneSchema,
})

export const updateCustomerSchema = z.object({
  fullName: fullNameSchema,
  email: emailSchema,
  phone: phoneSchema,
})

export const createDoctorSchema = z.object({
  fullName: fullNameSchema,
  username: usernameSchema,
  specialty: specialtySchema,
  slotMinutes: positiveInt,
  active: z.boolean(),
})

export const updateDoctorSchema = z.object({
  fullName: fullNameSchema,
  specialty: specialtySchema,
  slotMinutes: positiveInt,
  active: z.boolean(),
})

export const createScheduleSchema = z.object({
  doctorId: positiveInt,
  availableDate: z.string().min(1),
  startTime: timeRegex,
  endTime: timeRegex,
  capacity: positiveInt,
}).refine((data) => data.startTime < data.endTime, {
  message: 'End time must be after start time',
  path: ['endTime'],
})

export const scheduleFormSchema = z.object({
  availableDate: z.string().min(1),
  startTime: timeRegex,
  endTime: timeRegex,
  capacity: positiveInt,
}).refine((data) => data.startTime < data.endTime, {
  message: 'End time must be after start time',
  path: ['endTime'],
})

export const updateScheduleSchema = z.object({
  startTime: timeRegex,
  endTime: timeRegex,
  capacity: positiveInt,
}).refine((data) => data.startTime < data.endTime, {
  message: 'End time must be after start time',
  path: ['endTime'],
})

export const rescheduleSchema = z.object({
  newTime: timeRegex,
})

export const updateStatusSchema = z.object({
  status: z.enum(['BOOKED', 'CANCELLED', 'COMPLETED']),
})

export type LoginFormData = z.infer<typeof loginSchema>
export type CreateUserFormData = z.infer<typeof createUserSchema>
export type CreateCustomerFormData = z.infer<typeof createCustomerSchema>
export type UpdateCustomerFormData = z.infer<typeof updateCustomerSchema>
export type CreateDoctorFormData = z.infer<typeof createDoctorSchema>
export type UpdateDoctorFormData = z.infer<typeof updateDoctorSchema>
export type CreateScheduleFormData = z.infer<typeof createScheduleSchema>
export type UpdateScheduleFormData = z.infer<typeof updateScheduleSchema>
export type RescheduleFormData = z.infer<typeof rescheduleSchema>
export type UpdateStatusFormData = z.infer<typeof updateStatusSchema>
