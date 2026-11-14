const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

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

test("Unauthenticated user: cannot read user profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user: cannot read another user's profile", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Alice",
      phone: "+251911223344",
      city: "Addis Ababa",
      balanceEtb: 1000,
      gamesPlayed: 5,
      gamesWon: 2,
      totalEarningsEtb: 800,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user: can read their own profile", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Alice",
      phone: "+251911223344",
      city: "Addis Ababa",
      balanceEtb: 1000,
      gamesPlayed: 5,
      gamesWon: 2,
      totalEarningsEtb: 800,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("Multiplayer: Authenticated user can join room on multiple devices and count as 2 players", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const playersRef = aliceDb.collection("rooms").doc("mini_starter").collection("players");
  await assertSucceeds(
    playersRef.doc(`${ALICE_UID}_phone1`).set({
      userId: ALICE_UID,
      deviceId: "phone1",
      name: "Alice Phone",
      ticketNumber: 77,
      joinedAt: new Date(),
    })
  );
  await assertSucceeds(
    playersRef.doc(`${ALICE_UID}_tablet2`).set({
      userId: ALICE_UID,
      deviceId: "tablet2",
      name: "Alice Tablet",
      ticketNumber: 80,
      joinedAt: new Date(),
    })
  );
  const snapshot = await playersRef.get();
  if (snapshot.size !== 2) {
    throw new Error(`Expected 2 joined player docs, got ${snapshot.size}`);
  }
});

test("Multiplayer: Bob cannot impersonate or delete Alice's device entry", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("rooms").doc("mini_starter").collection("players").doc(`${ALICE_UID}_phone1`).set({
      userId: ALICE_UID,
      deviceId: "phone1",
      name: "Alice Player",
      ticketNumber: 77,
      joinedAt: new Date(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("rooms").doc("mini_starter").collection("players").doc(`${ALICE_UID}_phone2`).set({
      userId: ALICE_UID,
      deviceId: "phone2",
      name: "Imposter Alice",
      ticketNumber: 77,
      joinedAt: new Date(),
    })
  );
  await assertFails(
    bobDb.collection("rooms").doc("mini_starter").collection("players").doc(`${ALICE_UID}_phone1`).delete()
  );
});

test("Multiplayer: Authenticated user can create and read room with countdownStartedAt", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("rooms").doc("mini_starter").set({
      id: "mini_starter",
      name: "Mini Starter",
      stakeEtb: 10,
      prizeEtb: 16,
      houseCommissionEtb: 4,
      totalPoolEtb: 20,
      currentPlayers: 2,
      countdownSeconds: 180,
      countdownStartedAt: new Date(),
      status: "COUNTDOWN",
      calledNumbers: [],
      latestBall: 0,
      winnerId: "",
      winnerName: "",
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(aliceDb.collection("rooms").doc("mini_starter").get());
});

test("Account creation: authenticated user can store pinHash, promoCode, welcomeBonusClaimed, and WELCOME_BONUS tx", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Dawit Bekele",
      phone: "+251911223344",
      city: "Addis Ababa",
      balanceEtb: 350,
      gamesPlayed: 0,
      gamesWon: 0,
      totalEarningsEtb: 0,
      pinHash: "a".repeat(64),
      promoCode: "YEM100",
      welcomeBonusClaimed: true,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      pinHash: "b".repeat(64),
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_welcome_1").set({
      id: "tx_welcome_1",
      userId: ALICE_UID,
      type: "DEPOSIT",
      amountEtb: 100,
      status: "COMPLETED",
      paymentMethod: "WELCOME_BONUS",
      reference: "REGISTRATION_BONUS",
      createdAt: new Date(),
    })
  );

  // Bob cannot overwrite Alice's pinHash
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      pinHash: "c".repeat(64),
      updatedAt: new Date(),
    })
  );
});
