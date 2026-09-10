#!/usr/bin/env python3
"""Envoie un SMS via la passerelle locale (même Wi-Fi que le téléphone)."""

from __future__ import annotations

import argparse
import json
import sys
import urllib.error
import urllib.request


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Envoie un SMS par POST vers le téléphone sur le Wi-Fi local."
    )
    parser.add_argument("--url", required=True, help="URL affichée dans l'appli, ex. http://192.168.1.12:8765")
    parser.add_argument("--token", required=True, help="Jeton affiché dans l'appli")
    parser.add_argument("--tel", required=True, help="Numéro du destinataire")
    parser.add_argument("--message", required=True, help="Texte du SMS")
    args = parser.parse_args()

    endpoint = args.url.rstrip("/") + "/sms"
    payload = json.dumps({"tel": args.tel, "message": args.message}).encode("utf-8")
    request = urllib.request.Request(
        endpoint,
        data=payload,
        method="POST",
        headers={"Content-Type": "application/json", "X-Api-Key": args.token},
    )
    try:
        with urllib.request.urlopen(request, timeout=10) as response:
            body = json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")
        print(f"Erreur HTTP {error.code}: {detail}", file=sys.stderr)
        return 1
    except urllib.error.URLError as error:
        print(f"Impossible de joindre le téléphone: {error.reason}", file=sys.stderr)
        return 1

    print(json.dumps(body, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
