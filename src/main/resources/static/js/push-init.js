(function () {
    if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
        return;
    }

    var vapidMeta = document.getElementById('vapid-public-key');
    if (!vapidMeta) return;

    var vapidPublicKey = vapidMeta.getAttribute('content');
    if (!vapidPublicKey) return;

    var csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    var csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

    function urlBase64ToUint8Array(base64String) {
        var padding = '='.repeat((4 - base64String.length % 4) % 4);
        var base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
        var rawData = atob(base64);
        var outputArray = new Uint8Array(rawData.length);
        for (var i = 0; i < rawData.length; ++i) {
            outputArray[i] = rawData.charCodeAt(i);
        }
        return outputArray;
    }

    function sendSubscriptionToServer(subscription) {
        var keys = subscription.toJSON().keys;
        return fetch('/api/push/subscribe', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [csrfHeader]: csrfToken
            },
            body: JSON.stringify({
                endpoint: subscription.endpoint,
                p256dh: keys.p256dh,
                auth: keys.auth
            })
        });
    }

    navigator.serviceWorker.register('/sw.js').then(function (registration) {
        return registration.pushManager.getSubscription().then(function (existingSubscription) {
            if (existingSubscription) {
                return sendSubscriptionToServer(existingSubscription);
            }

            return Notification.requestPermission().then(function (permission) {
                if (permission !== 'granted') return;

                return registration.pushManager.subscribe({
                    userVisibleOnly: true,
                    applicationServerKey: urlBase64ToUint8Array(vapidPublicKey)
                }).then(function (newSubscription) {
                    return sendSubscriptionToServer(newSubscription);
                });
            });
        });
    }).catch(function (err) {
        console.warn('[PushInit] Errore registrazione service worker:', err);
    });
})();
