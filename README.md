# Passerelle SMS

Application Android à installer sur le téléphone. Elle écoute le Wi-Fi local, reçoit un POST du PC (numéro + message), met le SMS en file, l’envoie, et affiche l’état : **en attente**, **en cours**, **envoyé** ou **échec**.

Aucun serveur distant. Le PC et le téléphone doivent être sur le **même Wi-Fi**.

## Installer l’APK

1. Copiez `dist/passerelle-sms.apk` sur le téléphone.
2. Autorisez l’installation depuis cette source.
3. Ouvrez **Passerelle SMS**, acceptez l’envoi de SMS et les notifications.
4. Activez l’écoute (interrupteur). L’écran affiche l’URL (`http://192.168.x.x:8765`) et un **jeton**.

Gardez l’appli en avant-plan ou laissez la notification « Passerelle SMS » active. Désactivez l’optimisation batterie pour cette appli si Android coupe le serveur.

## Envoyer depuis le PC

Dans un navigateur, ouvrez l’URL affichée sur le téléphone, collez le jeton, puis envoyez.

Ou en ligne de commande :

```bash
curl -X POST "http://192.168.1.12:8765/sms" \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: VOTRE-JETON" \
  -d '{"tel":"+33612345678","message":"Bonjour depuis le PC"}'
```

Script fourni :

```bash
python3 pc/envoyer.py \
  --url "http://192.168.1.12:8765" \
  --token "VOTRE-JETON" \
  --tel "+33612345678" \
  --message "Bonjour depuis le PC"
```

Réponse : le SMS est **en attente** (`202`), puis l’appli l’envoie.

### Consulter la file

```bash
curl "http://192.168.1.12:8765/sms" -H "X-Api-Key: VOTRE-JETON"
curl "http://192.168.1.12:8765/sms/1" -H "X-Api-Key: VOTRE-JETON"
curl "http://192.168.1.12:8765/health" -H "X-Api-Key: VOTRE-JETON"
```

Champs du POST : `tel` (ou `phone` / `to`) et `message`. Un numéro français `06…` est converti en `+33…`.

## Sécurité

Le jeton est obligatoire. Sans lui, un voisin Wi-Fi pourrait envoyer des SMS avec votre forfait. Régénérez-le dans l’appli si besoin. Les requêtes hors réseau privé sont refusées.

## Compiler vous-même

Android SDK 34, JDK 17 ou 21.

```bash
export ANDROID_HOME=/chemin/vers/android-sdk
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew :app:assembleDebug
```

L’APK se trouve dans `app/build/outputs/apk/debug/`.

## Limites

- Usage personnel sur votre ligne, pas d’envoi de masse.
- « Envoyé » signifie que l’opérateur a accepté le SMS, pas que le destinataire l’a lu.
- Android peut tuer l’écoute en veille : gardez la notification, et l’écran allumé si besoin.
