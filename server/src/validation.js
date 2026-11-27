import { z } from "zod";
import { ApiError } from "./errors.js";

export const usernameSchema = z
  .string()
  .trim()
  .toLowerCase()
  .min(3)
  .max(30)
  .regex(/^[a-z0-9_.-]+$/, "username may only contain letters, numbers, dot, dash and underscore");

export const passwordSchema = z.string().min(8).max(100);
// Money crosses the API as a decimal string (e.g. "12.34") to avoid float rounding.
export const moneySchema = z.string().regex(/^\d{1,9}(\.\d{1,2})?$/);

export const signupSchema = z.object({
  username: usernameSchema,
  password: passwordSchema,
});

export const loginSchema = signupSchema;

export const createGroupSchema = z.object({
  name: z.string().trim().min(1).max(80),
  memberUsernames: z.array(usernameSchema).max(100).optional().default([]),
});

export const addMemberSchema = z.object({ username: usernameSchema });

export const createExpenseSchema = z.object({
  description: z.string().trim().min(1).max(120),
  amount: moneySchema,
  paidByUsername: usernameSchema,
  splitType: z.enum(["equal", "custom"]).optional().default("equal"),
  participantUsernames: z.array(usernameSchema).min(1).max(100).optional(),
  splits: z
    .array(z.object({ username: usernameSchema, amount: moneySchema }))
    .min(1)
    .max(100)
    .optional(),
});

export function validate(schema, data) {
  const result = schema.safeParse(data);
  if (result.success) return result.data;

  throw new ApiError(
    400,
    "Invalid request",
    result.error.issues.map((issue) => ({ path: issue.path.join("."), message: issue.message })),
  );
}
