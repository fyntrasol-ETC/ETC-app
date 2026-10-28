import fetch from "node-fetch";

interface SendPushOptions {
  externalUserIds?: string[];
  segments?: string[];
  title: string;
  body: string;
  data?: Record<string, string>;
  sendAfter?: string;
}

export async function sendPush(opts: SendPushOptions) {
  const payload: any = {
    app_id: process.env.ONESIGNAL_APP_ID,
    headings: { en: opts.title },
    contents: { en: opts.body },
    data: opts.data ?? {},
  };
  if (opts.externalUserIds) payload.include_external_user_ids = opts.externalUserIds;
  if (opts.segments) payload.included_segments = opts.segments;
  if (opts.sendAfter) payload.send_after = opts.sendAfter;

  const res = await fetch("https://onesignal.com/api/v1/notifications", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Basic ${process.env.ONESIGNAL_REST_API_KEY}`,
    },
    body: JSON.stringify(payload),
  });

  if (!res.ok) {
    console.error("OneSignal error:", await res.text());
  }
}