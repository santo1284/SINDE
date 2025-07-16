const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

exports.sendLikeNotification = functions.firestore
  .document("planes/{planId}/megusta/{userId}")
  .onCreate(async (snapshot, context) => {
    const planId = context.params.planId;
    const userId = context.params.userId;

    // Obtener los datos del plan para saber quién es el creador
    const planRef = admin.firestore().collection("planes").doc(planId);
    const planDoc = await planRef.get();
    if (!planDoc.exists) {
      return;
    }
    const planData = planDoc.data();
    const creatorId = planData.userId;

    // No enviar notificación si el usuario se da "me gusta" a sí mismo
    if (creatorId === userId) {
      return;
    }

    // Obtener el token FCM del creador del plan
    const creatorRef = admin.firestore().collection("usuarios").doc(creatorId);
    const creatorDoc = await creatorRef.get();
    if (!creatorDoc.exists) {
      return;
    }
    const creatorData = creatorDoc.data();
    const fcmToken = creatorData.fcmToken;

    if (!fcmToken) {
      return;
    }

    // Obtener el nombre del usuario que dio "me gusta"
    const userRef = admin.firestore().collection("usuarios").doc(userId);
    const userDoc = await userRef.get();
    if (!userDoc.exists) {
      return;
    }
    const userData = userDoc.data();
    const userName = userData.nombre;

    // Construir el mensaje de la notificación
    const payload = {
      notification: {
        title: "¡Nuevo me gusta!",
        body: `${userName} le ha dado me gusta a tu plan: ${planData.nombre}`,
      },
    };

    // Enviar la notificación
    try {
      await admin.messaging().sendToDevice(fcmToken, payload);
    } catch (error) {
      console.error("Error sending notification:", error);
    }
  });

exports.sendParticipantNotification = functions.firestore
  .document("planes/{planId}/participantes/{userId}")
  .onCreate(async (snapshot, context) => {
    const planId = context.params.planId;
    const userId = context.params.userId;

    // Obtener los datos del plan para saber quién es el creador
    const planRef = admin.firestore().collection("planes").doc(planId);
    const planDoc = await planRef.get();
    if (!planDoc.exists) {
      return;
    }
    const planData = planDoc.data();
    const creatorId = planData.userId;

    // No enviar notificación si el creador se une a su propio plan
    if (creatorId === userId) {
      return;
    }

    // Obtener el token FCM del creador del plan
    const creatorRef = admin.firestore().collection("usuarios").doc(creatorId);
    const creatorDoc = await creatorRef.get();
    if (!creatorDoc.exists) {
      return;
    }
    const creatorData = creatorDoc.data();
    const fcmToken = creatorData.fcmToken;

    if (!fcmToken) {
      return;
    }

    // Obtener el nombre del usuario que se unió
    const userRef = admin.firestore().collection("usuarios").doc(userId);
    const userDoc = await userRef.get();
    if (!userDoc.exists) {
      return;
    }
    const userData = userDoc.data();
    const userName = userData.nombre;

    // Construir el mensaje de la notificación
    const payload = {
      notification: {
        title: "¡Nuevo participante!",
        body: `${userName} se ha unido a tu plan: ${planData.nombre}`,
      },
    };

    // Enviar la notificación
    try {
      await admin.messaging().sendToDevice(fcmToken, payload);
    } catch (error) {
      console.error("Error sending notification:", error);
    }
  });
