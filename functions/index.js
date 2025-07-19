const functions = require('firebase-functions');
const admin = require('firebase-admin');

// Inicializar Firebase Admin
admin.initializeApp();

// Cloud Function que se ejecuta cuando se crea una nueva notificación
exports.sendPushNotification = functions.firestore
  .document('notifications/{notificationId}')
  .onCreate(async (snap, context) => {
    try {
      // Obtener los datos de la notificación
      const notificationData = snap.data();
      const { fcmToken, senderName, message, type, planId, planTitle } = notificationData;

      // Verificar que tenemos un token FCM
      if (!fcmToken) {
        console.log('No FCM token found for this notification');
        return null;
      }

      // Crear el payload de la notificación push
      const payload = {
        token: fcmToken,
        notification: {
          title: getNotificationTitle(type, senderName),
          body: planTitle,
        },
        data: {
          notificationId: context.params.notificationId,
          planId: planId,
          type: type,
          senderId: notificationData.senderId,
          click_action: 'FLUTTER_NOTIFICATION_CLICK'
        },
        android: {
          notification: {
            icon: 'ic_notification',
            color: '#FF5722',
            sound: 'default',
            clickAction: 'FLUTTER_NOTIFICATION_CLICK'
          }
        },
        apns: {
          payload: {
            aps: {
              sound: 'default',
              badge: 1
            }
          }
        }
      };

      // Enviar la notificación push
      const response = await admin.messaging().send(payload);
      console.log('Successfully sent message:', response);

      // Actualizar el documento de notificación para marcar que se envió
      await snap.ref.update({
        pushSent: true,
        pushSentAt: admin.firestore.FieldValue.serverTimestamp()
      });

      return response;

    } catch (error) {
      console.error('Error sending push notification:', error);

      // Marcar que hubo un error al enviar
      await snap.ref.update({
        pushSent: false,
        pushError: error.message,
        pushSentAt: admin.firestore.FieldValue.serverTimestamp()
      });

      return null;
    }
  });

// Función helper para obtener el título según el tipo
function getNotificationTitle(type, senderName) {
  switch (type) {
    case 'like':
      return `¡A ${senderName} le gustó tu plan!`;
    case 'participate':
      return `${senderName} va a participar`;
    case 'share':
      return `${senderName} compartió tu plan`;
    case 'comment':
      return `${senderName} comentó tu plan`;
    default:
      return `${senderName} interactuó contigo`;
  }
}

// Cloud Function para limpiar notificaciones antiguas (opcional)
exports.cleanupOldNotifications = functions.pubsub
  .schedule('every 24 hours')
  .onRun(async (context) => {
    const cutoff = new Date();
    cutoff.setDate(cutoff.getDate() - 30); // 30 días atrás

    const snapshot = await admin.firestore()
      .collection('notifications')
      .where('timestamp', '<', cutoff)
      .get();

    const batch = admin.firestore().batch();
    snapshot.docs.forEach((doc) => {
      batch.delete(doc.ref);
    });

    await batch.commit();
    console.log(`Deleted ${snapshot.size} old notifications`);
    return null;
  });