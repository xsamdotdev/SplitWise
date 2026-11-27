import test from "node:test";
import assert from "node:assert/strict";
import { parseAmountCents, formatAmountCents, splitEqually } from "../src/money.js";

test("parses money strings into exact cents", () => {
  assert.equal(parseAmountCents("12.30"), 1230);
  assert.equal(parseAmountCents("7"), 700);
  assert.equal(parseAmountCents("0.05"), 5);
});

test("rejects invalid and zero amounts", () => {
  assert.throws(() => parseAmountCents("0"), /greater than zero/);
  assert.throws(() => parseAmountCents("1.234"), /2 decimal places/);
  assert.throws(() => parseAmountCents("abc"), /2 decimal places/);
});

test("formats cents", () => {
  assert.equal(formatAmountCents(1230), "12.30");
  assert.equal(formatAmountCents(-5), "-0.05");
});

test("equal splits always sum to the total", () => {
  assert.deepEqual(splitEqually(1000, ["a", "b", "c"]), [
    { userId: "a", amountCents: 334 },
    { userId: "b", amountCents: 333 },
    { userId: "c", amountCents: 333 },
  ]);
  assert.equal(
    splitEqually(555, ["a", "b", "c", "d", "e", "f", "g"])
      .reduce((total, split) => total + split.amountCents, 0),
    555,
  );
});
