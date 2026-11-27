import { ApiError } from "./errors.js";

const AMOUNT_PATTERN = /^\d{1,9}(\.\d{1,2})?$/;

/** Convert a decimal money string (e.g. "12.30") into integer cents. */
export function parseAmountCents(value, label = "amount") {
  let text = value;
  if (typeof text === "number") {
    if (!Number.isFinite(text)) throw badAmount(label);
    text = text.toFixed(2);
  }
  if (typeof text !== "string") throw badAmount(label);

  text = text.trim();
  if (!AMOUNT_PATTERN.test(text)) throw badAmount(label);

  const [whole, fraction = ""] = text.split(".");
  const cents = Number(whole) * 100 + Number(fraction.padEnd(2, "0"));
  if (cents <= 0) throw new ApiError(400, `${label} must be greater than zero`);
  return cents;
}

function badAmount(label) {
  return new ApiError(400, `${label} must be a positive amount with at most 2 decimal places`);
}

/** Integer cents to a fixed-width decimal string, e.g. 1230 -> "12.30". */
export function formatAmountCents(cents) {
  const sign = cents < 0 ? "-" : "";
  const absolute = Math.abs(cents);
  return `${sign}${Math.floor(absolute / 100)}.${String(absolute % 100).padStart(2, "0")}`;
}

/**
 * Split a total into exact integer cents. Remainder cents go to the first
 * participants so that the parts always add up to the total.
 */
export function splitEqually(totalCents, userIds) {
  if (!Array.isArray(userIds) || userIds.length === 0) {
    throw new ApiError(400, "Select at least one participant");
  }
  const base = Math.floor(totalCents / userIds.length);
  let remainder = totalCents - base * userIds.length;
  return userIds.map((userId) => {
    const extra = remainder > 0 ? 1 : 0;
    remainder -= extra;
    return { userId, amountCents: base + extra };
  });
}
