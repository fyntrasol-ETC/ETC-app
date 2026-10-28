import * as admin from "firebase-admin";

const db = admin.firestore();

export async function writeNotifDoc(uid: string, notif: {
  type: string;
  title: string;
  body: string;
  deepLink?: string;
}) {
  await db.collection("users").doc(uid).collection("notifications").add({
    ...notif,
    read: false,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });
}