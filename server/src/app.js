import express from "express";
import cors from "cors";
import { authRouter } from "./routes/auth.js";
import { groupsRouter } from "./routes/groups.js";
import { ApiError } from "./errors.js";

export function createApp() {
  const app = express();
  app.use(cors());
  app.use(express.json({ limit: "64kb" }));

  app.get("/health", (_req, res) => res.json({ status: "ok" }));
  app.use("/auth", authRouter);
  app.use("/groups", groupsRouter);

  app.use((_req, _res, next) => next(new ApiError(404, "Endpoint not found")));

  app.use((error, _req, res, _next) => {
    if (error?.code === "P2002") {
      return res.status(409).json({ error: "That value is already in use" });
    }
    if (error instanceof SyntaxError && "body" in error) {
      return res.status(400).json({ error: "Invalid JSON body" });
    }

    const status = error instanceof ApiError ? error.status : 500;
    if (status >= 500) console.error(error);
    res.status(status).json({ error: error.message ?? "Internal server error", details: error.details });
  });

  return app;
}
