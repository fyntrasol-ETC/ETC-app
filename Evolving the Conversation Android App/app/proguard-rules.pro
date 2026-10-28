# Add project specific ProGuard rules here.

# ── Firebase Firestore POJO models ──────────────────────────────────────
# doc.toObject(SomeClass::class.java) uses reflection to match field names
# and requires a no-arg constructor. R8 renames/strips fields and
# constructors by default, which silently breaks deserialization in
# release builds only (works fine in debug where nothing is obfuscated).
# CommunityScreen crashes because CommunityViewModel does exactly this
# for every post/comment the moment you open the tab.
-keepclassmembers class com.evolvingtheconversation.app.viewmodel.CommunityPost {
    <init>(...);
    *;
}
-keepclassmembers class com.evolvingtheconversation.app.viewmodel.CommentItem {
    <init>(...);
    *;
}

# If you have any other data classes anywhere in the app that are passed to
# doc.toObject(...) or snapshot.toObjects(...), add them here the same way.
# Quick way to find them all: search the project for ".toObject(" and
# ".toObjects(".

-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# ── Kotlin metadata (helps reflection-based libraries, incl. Firestore) ──
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata { *; }

# ── Google Sign-In / Credential Manager / GoogleID ───────────────────────
-keep class com.google.android.libraries.identity.googleid.** { *; }
-keep class androidx.credentials.** { *; }

# ── OneSignal (their docs recommend this) ────────────────────────────────
-keep class com.onesignal.** { *; }
-dontwarn com.onesignal.**

# If your app uses no other reflection-based JSON/parsing beyond Firestore,
# you generally don't need broader rules — Firebase's own AAR ships
# consumer ProGuard rules for its internal classes automatically.