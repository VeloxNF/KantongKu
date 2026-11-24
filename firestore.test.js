const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_001";
const BOB_UID = "bob_uid_002";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated access is denied across users, pockets, and transactions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("pockets").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("pockets").doc("p_1").set({
      userId: ALICE_UID,
      name: "Makan/Minum",
      mode: "BUDGET_LIMIT",
      targetAmount: 1500000,
      currentAmount: 650000,
      iconKey: "FOOD",
      colorSlot: 0,
      featured: true,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Owner can create, read, and update user profile; cross-user and tampered writes fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Kia",
      paydayDay: 25,
      monthlyBudget: 3500000,
      themeScheme: "BENTO_NEON",
      themeMode: "DARK",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ displayName: "Hacked" }));

  // Update displayName + updatedAt while preserving createdAt
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Kia Pratama",
      themeScheme: "EMERALD_MINT",
      themeMode: "LIGHT",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Tampering with createdAt must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Kia",
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Listing /users is denied
  await assertFails(aliceDb.collection("users").get());
});

test("Owner can create, list, update, and delete pockets; extra id key or cross-user access fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("pockets").doc("pocket_food").set({
      userId: ALICE_UID,
      name: "Makan/Minum",
      mode: "BUDGET_LIMIT",
      targetAmount: 1500000,
      currentAmount: 650000,
      iconKey: "FOOD",
      colorSlot: 0,
      featured: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // List subcollection (even empty or populated) succeeds for owner
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("pockets").get());

  // Partial update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("pockets").doc("pocket_food").update({
      currentAmount: 700000,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Including @DocumentId `id` key in body fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("pockets").doc("pocket_2").set({
      id: "pocket_2",
      userId: ALICE_UID,
      name: "Transport",
      mode: "BUDGET_LIMIT",
      targetAmount: 500000,
      currentAmount: 0,
      iconKey: "TRANSPORT",
      colorSlot: 1,
      featured: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Future timestamp fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("pockets").doc("pocket_future").set({
      userId: ALICE_UID,
      name: "Future",
      mode: "SAVINGS_GOAL",
      targetAmount: 1000000,
      currentAmount: 0,
      iconKey: "SAVINGS",
      colorSlot: 2,
      featured: false,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Cross-user read/write fails
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("pockets").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("pockets").doc("pocket_food").delete());

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("pockets").doc("pocket_food").delete());
});

test("Owner can create, list, update, and delete transactions with detailed notes; cross-user fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").set({
      userId: ALICE_UID,
      title: "Makan Siang",
      note: "Nasi padang rendang 25rb, es teh manis 5rb, kerupuk 3rb",
      amount: 33000,
      type: "EXPENSE",
      pocketId: "pocket_food",
      pocketName: "Makan/Minum",
      occurredAtMillis: Date.now() - 3600000,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").orderBy("occurredAtMillis", "desc").get()
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").update({
      note: "Nasi padang rendang 25rb, es teh manis 5rb, kerupuk 3rb, parkir 2rb",
      amount: 35000,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Invalid type or zero amount fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_bad").set({
      userId: ALICE_UID,
      title: "Bad",
      note: "",
      amount: 0,
      type: "EXPENSE",
      pocketId: "",
      pocketName: "",
      occurredAtMillis: Date.now(),
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("transactions").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").delete());
});
