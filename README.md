# Splitwise clone

A small Splitwise-style app: username/password auth (no email or phone), groups, shared expenses, and balances.

- `server/` — Express + Prisma + PostgreSQL API
- `client/` — Android app written in Kotlin with Jetpack Compose

## Server setup

```bash
cd server
npm install
cp .env.example .env        # set a long random JWT_SECRET
docker compose up -d        # PostgreSQL on localhost:5433 (5432 is often already taken)
npm run db:migrate          # apply the checked-in migration
npm run dev                 # http://localhost:3000
```

Useful commands: `npm run db:studio`, `npm test`, `npm run smoke` (end-to-end check against the running API), and
`npm run db:dev` (create a new migration after editing the schema).

## API

All `/groups` routes require `Authorization: Bearer <token>`.

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/auth/signup` | `{ username, password }` → `{ token, user }` |
| POST | `/auth/login` | `{ username, password }` → `{ token, user }` |
| GET | `/auth/me` | Current user |
| GET | `/groups` | Groups with the current user's balance |
| POST | `/groups` | `{ name, memberUsernames?: [] }` |
| GET | `/groups/:groupId` | Members, expenses, balances, simplified debts |
| POST | `/groups/:groupId/members` | `{ username }` |
| POST | `/groups/:groupId/expenses` | `{ description, amount, paidByUsername, splitType, participantUsernames?, splits? }` |
| DELETE | `/groups/:groupId/expenses/:expenseId` | Remove an expense (payer or group creator only) |

`amount` is a decimal string such as `"12.34"`. Internally all money is stored as integer cents.
`splitType: "equal"` splits across selected members (all members by default); `"custom"` uses
`splits: [{ username, amount }]`, which must add up to the expense total.

Balances are derived from expenses. Positive `netCents` means the user is owed money; negative means they owe.
`simplifiedDebts` is the smallest set of transfers that settles everybody.

## Android client setup

Open `client/` in Android Studio and run the `app` configuration on an emulator or device.

The app points at `http://10.0.2.2:3000/` by default (Android emulator → host machine). Override it for a
physical device or another host with:

```bash
./gradlew installDebug -PapiBaseUrl=http://192.168.1.10:3000/
```

Cleartext HTTP is enabled only in the debug manifest. The token is stored in SharedPreferences.
The `client/` folder was pre-existing; this app builds on that Compose scaffold.
