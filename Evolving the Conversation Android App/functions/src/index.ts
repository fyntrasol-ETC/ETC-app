import * as admin from "firebase-admin";
import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { onSchedule } from "firebase-functions/v2/scheduler";

admin.initializeApp();
const db = admin.firestore();

import { sendPush } from "./onesignal";
import { writeNotifDoc } from "./notifDoc";

const secrets = ["ONESIGNAL_APP_ID", "ONESIGNAL_REST_API_KEY"];

// 1) Daily challenge refresh
export const dailyChallengeRefresh = onSchedule(
  { schedule: "0 0 * * *", timeZone: "Asia/Karachi", secrets },
  async () => {
    await sendPush({
      segments: ["All"],
      title: "New Daily Challenge! 🔥",
      body: "Today's challenge is live — keep your streak going.",
    });
  }
);

// 2) "1 hour left" reminder
export const challengeReminderHourLeft = onSchedule(
  { schedule: "0 23 * * *", timeZone: "Asia/Karachi", secrets },
  async () => {
    const today = new Date().toISOString().slice(0, 10);
    const usersSnap = await db.collection("users").get();
    const targets: string[] = [];

    for (const u of usersSnap.docs) {
      const ch = await u.ref.collection("dailyChallenges").doc(today).get();
      if (!ch.exists || ch.data()?.isCompleted === false) {
        targets.push(u.id);
      }
    }

    if (targets.length === 0) return;

    await sendPush({
      externalUserIds: targets,
      title: "⏰ 1 hour left!",
      body: "You still have 1 hour to finish today's challenge.",
    });

    for (const uid of targets) {
      await writeNotifDoc(uid, {
        type: "daily_challenge_reminder",
        title: "1 hour left!",
        body: "You still have 1 hour to finish today's challenge.",
      });
    }
  }
);

// 3) Someone posted in Community
export const onNewCommunityPost = onDocumentCreated(
  { document: "community_posts/{postId}", secrets },
  async (event) => {
    const post = event.data?.data();
    if (!post) return;

    const optedIn = await db.collection("users")
      .where("communityPushOptIn", "==", true).get();

    const targets = optedIn.docs
      .map((d) => d.id)
      .filter((id) => id !== post.authorUid);

    if (targets.length === 0) return;

    await sendPush({
      externalUserIds: targets,
      title: "New post in Community",
      body: String(post.text).slice(0, 80),
      data: { deepLink: `community/post/${event.params.postId}` },
    });

    for (const uid of targets) {
      await writeNotifDoc(uid, {
        type: "community_new_post",
        title: "New post in Community",
        body: String(post.text).slice(0, 80),
        deepLink: `community/post/${event.params.postId}`,
      });
    }
  }
);

// 4) Reply to your post or comment
export const onNewComment = onDocumentCreated(
  { document: "community_posts/{postId}/comments/{commentId}", secrets },
  async (event) => {
    const comment = event.data?.data();
    if (!comment) return;

    const postRef = db.collection("community_posts").doc(event.params.postId);
    let recipientUid: string | undefined;

    if (comment.parentCommentId) {
      const parent = await postRef.collection("comments").doc(comment.parentCommentId).get();
      recipientUid = parent.data()?.authorUid;
    } else {
      const post = await postRef.get();
      recipientUid = post.data()?.authorUid;
    }

    if (!recipientUid || recipientUid === comment.authorUid) return;

    await sendPush({
      externalUserIds: [recipientUid],
      title: "New reply",
      body: String(comment.text).slice(0, 80),
      data: { deepLink: `community/post/${event.params.postId}` },
    });

    await writeNotifDoc(recipientUid, {
      type: "reply_to_you",
      title: "New reply",
      body: String(comment.text).slice(0, 80),
      deepLink: `community/post/${event.params.postId}`,
    });
  }
);

// 5) Random motivational/funny text, rotating through sections
const SECTIONS = ["speaking", "listening", "writing", "grammar", "vocabulary"];

export const dailyMotivationalPush = onSchedule(
  { schedule: "0 10 * * *", timeZone: "Asia/Karachi", secrets },
  async () => {
    const stateRef = db.collection("notificationRotation").doc("state");
    const stateSnap = await stateRef.get();
    const state = stateSnap.exists
      ? stateSnap.data()!
      : { lastSectionIndex: -1, usedMessageIds: {} as Record<string, number[]> };

    const nextIndex = (state.lastSectionIndex + 1) % SECTIONS.length;
    const section = SECTIONS[nextIndex];

    const poolSnap = await db.collection("notificationPool").doc(section).get();
    const pool: string[] = poolSnap.data()?.messages ?? [];
    if (pool.length === 0) return;

    const used: number[] = state.usedMessageIds[section] ?? [];
    let available = pool.map((_, i) => i).filter((i) => !used.includes(i));

    if (available.length === 0) {
      available = pool.map((_, i) => i);
      state.usedMessageIds[section] = [];
    }

    const pickIndex = available[Math.floor(Math.random() * available.length)];
    const message = pool[pickIndex];

    await sendPush({ segments: ["All"], title: "✨", body: message });

    await stateRef.set({
      lastSectionIndex: nextIndex,
      usedMessageIds: {
        ...state.usedMessageIds,
        [section]: [...used, pickIndex],
      },
    });
  }
);

// 6) Live class added — immediate ping + reminder 1hr before
export const onLiveClassCreated = onDocumentCreated(
  { document: "liveClasses/{classId}", secrets },
  async (event) => {
    const cls = event.data?.data();
    if (!cls) return;

    await sendPush({
      segments: ["All"],
      title: "New live class scheduled",
      body: `${cls.title} — check the calendar for the time.`,
    });

    const classDate: admin.firestore.Timestamp = cls.date;
    const reminderTime = new Date(classDate.toMillis() - 60 * 60 * 1000).toISOString();

    await sendPush({
      segments: ["All"],
      title: "Class starts in 1 hour",
      body: cls.title,
      sendAfter: reminderTime,
    });
  }
);