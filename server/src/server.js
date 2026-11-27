import { config } from "./config.js";
import { createApp } from "./app.js";
import { prisma } from "./prisma.js";

if (!config.jwtSecret) {
  throw new Error("JWT_SECRET is missing. Copy .env.example to .env and set a long random secret.");
}

const app = createApp();
// Bind to all IPv4 interfaces so the Android emulator (10.0.2.2) and
// physical devices on the LAN can reach the API, not just localhost.
const server = app.listen(config.port, "0.0.0.0", () => {
  console.log(`Splitwise API listening on http://localhost:${config.port}`);
});

async function shutdown() {
  server.close();
  await prisma.$disconnect();
  process.exit(0);
}

process.on("SIGINT", shutdown);
process.on("SIGTERM", shutdown);
