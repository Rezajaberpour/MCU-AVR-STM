const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_engineer_1";
const BOB_UID = "bob_engineer_2";

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

test("Unauthenticated user cannot read or write projects or diagnostics", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("projects").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("diagnostics").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").set({
      userId: ALICE_UID,
      title: "PWM Driver",
      mcuFamily: "AVR",
      mcuModel: "ATmega328P",
      category: "CODE",
      promptText: "Generate Timer1 PWM",
      codeOrResult: "int main(void) { return 0; }",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Owner can create, list, partial update, and delete McuProjectRecord", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);
  const projectsRef = aliceDb.collection("users").doc(ALICE_UID).collection("projects");

  await assertSucceeds(
    projectsRef.doc("proj_avr_1").set({
      userId: ALICE_UID,
      title: "ATmega328P Fast PWM",
      mcuFamily: "AVR",
      mcuModel: "ATmega328P",
      category: "CODE",
      promptText: "Timer1 20kHz PWM",
      codeOrResult: "#include <avr/io.h>\nint main(void){}",
      notes: "Atmel Studio 7 -Os",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(projectsRef.get());

  await assertSucceeds(
    projectsRef.doc("proj_avr_1").update({
      title: "ATmega328P 20kHz PWM v2",
      notes: "Optimized for low power",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt modification
  await assertFails(
    projectsRef.doc("proj_avr_1").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field / @DocumentId 'id' injection
  await assertFails(
    projectsRef.doc("proj_avr_2").set({
      id: "proj_avr_2",
      userId: ALICE_UID,
      title: "Extra ID",
      mcuFamily: "STM32",
      mcuModel: "STM32F103C8",
      category: "CODE",
      promptText: "SPI DMA",
      codeOrResult: "void init() {}",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject invalid mcuFamily enum
  await assertFails(
    projectsRef.doc("proj_bad_enum").set({
      userId: ALICE_UID,
      title: "Bad Family",
      mcuFamily: "ESP32",
      mcuModel: "ESP32-S3",
      category: "CODE",
      promptText: "Test",
      codeOrResult: "void app() {}",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(projectsRef.doc("proj_avr_1").delete());
});

test("Owner can create, list, update, and delete CircuitDiagnosticRecord", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);
  const diagRef = aliceDb.collection("users").doc(ALICE_UID).collection("diagnostics");

  await assertSucceeds(
    diagRef.doc("diag_1").set({
      userId: ALICE_UID,
      title: "Buck Converter Schematic Audit",
      mcuFamily: "STM32",
      diagnosticType: "SCHEMATIC_VISION",
      queryText: "Check 3.3V regulator decoupling",
      analysisResult: "Add 100nF ceramic cap close to VDD pins.",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(diagRef.get());

  await assertSucceeds(
    diagRef.doc("diag_1").update({
      analysisResult: "Add 100nF X7R ceramic cap + 10uF bulk cap.",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject future timestamp
  await assertFails(
    diagRef.doc("diag_future").set({
      userId: ALICE_UID,
      title: "Future timestamp test",
      mcuFamily: "GENERAL",
      diagnosticType: "STANDARDS_SEARCH",
      queryText: "IPC-2221",
      analysisResult: "Trace width check",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Cross-user isolation blocks Bob from reading or writing Alice's data", async () => {
  const created = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("projects").doc("alice_proj").set({
      userId: ALICE_UID,
      title: "Alice Firmware",
      mcuFamily: "HOLTEK",
      mcuModel: "HT66F018",
      category: "CODE",
      promptText: "ADC read",
      codeOrResult: "void main() {}",
      createdAt: created,
      updatedAt: created,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("projects").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("projects").doc("alice_proj").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("projects").doc("alice_proj").update({
      title: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("projects").doc("alice_proj").delete());
});
