import { Router } from "express";
import bcrypt from "bcryptjs";
import { prisma } from "../prisma.js";
import { asyncHandler, ApiError } from "../errors.js";
import { requireAuth, signToken } from "../auth.js";
import { validate, signupSchema, loginSchema } from "../validation.js";

export const authRouter = Router();

function publicUser(user) {
  return { id: user.id, username: user.username, createdAt: user.createdAt };
}

authRouter.post(
  "/signup",
  asyncHandler(async (req, res) => {
    const { username, password } = validate(signupSchema, req.body);

    const existing = await prisma.user.findUnique({ where: { username } });
    if (existing) throw new ApiError(409, "That username is already taken");

    const passwordHash = await bcrypt.hash(password, 12);
    const user = await prisma.user.create({ data: { username, passwordHash } });
    res.status(201).json({ token: signToken(user), user: publicUser(user) });
  }),
);

authRouter.post(
  "/login",
  asyncHandler(async (req, res) => {
    const { username, password } = validate(loginSchema, req.body);

    const user = await prisma.user.findUnique({ where: { username } });
    if (!user || !(await bcrypt.compare(password, user.passwordHash))) {
      throw new ApiError(401, "Invalid username or password");
    }

    res.json({ token: signToken(user), user: publicUser(user) });
  }),
);

authRouter.get(
  "/me",
  requireAuth,
  asyncHandler(async (req, res) => {
    res.json({ user: publicUser(req.user) });
  }),
);
