"""Texts for answers that come from the service logic (errors, mails). Page texts sit next to their HTML in pages.py as
(English, Deutsch) pairs. American English; German parallel (AGENTS rule 5)."""
from __future__ import annotations

TEXTS = {
    # errors
    "bad_request": ("The request was not understood.", "Die Anfrage war nicht verständlich."),
    "not_found": ("Not found.", "Nicht gefunden."),
    "forbidden": ("You may not do this.", "Das darfst du nicht."),
    "login_needed": ("Please log in first.", "Bitte zuerst anmelden."),
    "csrf": ("The form expired. Please reload the page and try again.", "Das Formular ist abgelaufen. Bitte Seite neu laden und noch einmal versuchen."),
    "too_many": ("Too many attempts. Please wait a moment.", "Zu viele Versuche. Bitte kurz warten."),
    "too_large": ("Too large.", "Zu groß."),
    "register_closed": ("Registration is closed at the moment.", "Die Registrierung ist gerade geschlossen."),
    "email_invalid": ("Please enter a valid e-mail address.", "Bitte eine gültige E-Mail-Adresse eingeben."),
    "pw_short": ("The password needs at least 10 characters.", "Das Passwort braucht mindestens 10 Zeichen."),
    "pw_long": ("The password is too long (at most 256 characters).", "Das Passwort ist zu lang (höchstens 256 Zeichen)."),
    "pw_common": ("This password is too easy to guess.", "Dieses Passwort ist zu leicht zu erraten."),
    "pw_email": ("The password must not be your e-mail address.", "Das Passwort darf nicht deine E-Mail-Adresse sein."),
    "pw_wrong": ("The password is not correct.", "Das Passwort stimmt nicht."),
    "pw_mismatch": ("The two passwords differ.", "Die beiden Passwörter sind verschieden."),
    "name_invalid": ("Please enter a name (at most 40 characters).", "Bitte einen Namen eingeben (höchstens 40 Zeichen)."),
    "login_failed": ("E-mail or password is not correct.", "E-Mail oder Passwort stimmt nicht."),
    "login_locked": ("Too many failed logins. Try again in {minutes} minutes.", "Zu viele Fehlversuche. Bitte in {minutes} Minuten noch einmal."),
    "link_invalid": ("This link is invalid or expired.", "Dieser Link ist ungültig oder abgelaufen."),
    "verify_first": ("Please confirm your e-mail address first.", "Bitte bestätige zuerst deine E-Mail-Adresse."),
    "no_slot": ("All {max} places of your tier are in use. Disconnect a map or choose a higher tier.",
                "Alle {max} Plätze deiner Stufe sind belegt. Trenne eine Karte oder wähle eine höhere Stufe."),
    "slot_locked": ("A free place can be taken again at most once per 7 days. Next free place: {date}.",
                    "Ein freier Platz lässt sich höchstens einmal pro 7 Tage neu belegen. Nächster freier Platz: {date}."),
    "no_public": ("Your tier allows {max} public map(s). Choose private or link.", "Deine Stufe erlaubt {max} öffentliche Karte(n). Wähle privat oder Link."),
    "custom_name_tier": ("An own address name comes with Basic and higher.", "Ein eigener Adressname gehört zu Basic und höher."),
    "slug_invalid": ("Address name: 3-32 characters a-z, 0-9 and -, not a reserved word.", "Adressname: 3-32 Zeichen a-z, 0-9 und -, kein reserviertes Wort."),
    "slug_taken": ("This address name is taken.", "Dieser Adressname ist vergeben."),
    "too_many_codes": ("You already have 3 open codes. Use one or wait 15 minutes.", "Du hast schon 3 offene Codes. Nutze einen oder warte 15 Minuten."),
    "code_invalid": ("This is no connect code (8 letters/digits).", "Das ist kein Verbindungscode (8 Buchstaben/Ziffern)."),
    "code_unknown": ("Unknown or expired code. Make a new one on {host}.", "Unbekannter oder abgelaufener Code. Erzeuge einen neuen auf {host}."),
    "world_invalid": ("The world id is missing.", "Die Welt-ID fehlt."),
    "world_connected": ("This world is already connected to one of your maps.", "Diese Welt ist schon mit einer deiner Karten verbunden."),
    "key_unknown": ("Unknown map key.", "Unbekannter Kartenschlüssel."),
    "map_disconnected": ("The map was disconnected.", "Die Karte wurde getrennt."),
    "queued": ("Waiting in the queue (place {position}).", "Wartet in der Warteschlange (Platz {position})."),
    "bundle_invalid": ("Only 128 x 128 PNG map tiles are accepted.", "Nur Kartenkacheln (PNG, 128 × 128) werden angenommen."),
    "map_full": ("The storage of this map is full for your tier.", "Der Speicher dieser Karte ist für deine Stufe voll."),
    "storage_full": ("The service storage is full at the moment.", "Der Speicher des Dienstes ist gerade voll."),
    "share_unknown": ("There is no other account with this e-mail address.", "Es gibt kein anderes Konto mit dieser E-Mail-Adresse."),
    "share_limit": ("Your tier shares a map with at most {max} accounts.", "Deine Stufe teilt eine Karte mit höchstens {max} Konten."),
    "dpa_needed": ("To show players, accept the processing agreement for player data first (box below the switch).",
                   "Um Spieler zu zeigen, zuerst die Vereinbarung zur Verarbeitung der Spielerdaten annehmen (Kästchen unter dem Schalter)."),
    "coming_soon": ("Paying is coming soon.", "Bezahlen kommt bald."),
    "signature": ("Bad signature.", "Ungültige Signatur."),
    "payment_error": ("The payment service did not answer. Nothing was charged; please try again later.",
                      "Der Zahlungsdienst hat nicht geantwortet. Es wurde nichts abgebucht; bitte später noch einmal versuchen."),
    "terms_needed": ("Please accept the terms and the withdrawal notice (first box).", "Bitte AGB und Widerrufsbelehrung akzeptieren (erstes Kästchen)."),
    "sub_exists": ("You already have a subscription. Change or cancel it in the customer portal.",
                   "Du hast schon ein Abo. Wechseln oder kündigen kannst du im Kundenportal."),
    "sub_active_delete": ("Your subscription is still running. Cancel it first (it ends with the paid period), then delete the account.",
                          "Dein Abo läuft noch. Kündige es zuerst (es endet mit dem bezahlten Zeitraum), dann kannst du das Konto löschen."),
    "lite_none": ("Your plan has no fellow-player seats.", "Deine Stufe hat keine Mitspieler-Plätze."),
    "lite_target": ("This account cannot get a seat (it must be another confirmed account without own subscription or seat).",
                    "Dieses Konto kann keinen Platz bekommen (es muss ein anderes bestätigtes Konto ohne eigenes Abo oder Platz sein)."),
    "lite_full": ("All seats are taken. A freed seat can be given again after {days} days.", "Alle Plätze sind vergeben. Ein freigewordener Platz ist erst nach {days} Tagen wieder vergebbar."),
    "coupon_invalid": ("This coupon code is not valid.", "Dieser Gutscheincode ist nicht gültig."),
    "gift_code_invalid": ("This gift code is not valid (or the gift has ended).", "Dieser Geschenkcode ist nicht gültig (oder das Geschenk ist abgelaufen)."),
    "gift_code_used": ("This gift code has already been redeemed.", "Dieser Geschenkcode wurde schon eingelöst."),
    "mc_name_invalid": ("A Minecraft name has 3-16 characters: letters, digits, _.", "Ein Minecraft-Name hat 3–16 Zeichen: Buchstaben, Ziffern, _."),
    "mc_name_count": ("This plan allows {max} Minecraft name(s).", "Diese Stufe erlaubt {max} Minecraft-Namen."),
    "mc_name_wait": ("The name can be changed again on {date} (every 7 days).", "Der Name ist ab {date} wieder änderbar (alle 7 Tage)."),
    "contact_message": ("Please write a message (10-5000 characters).", "Bitte eine Nachricht schreiben (10–5000 Zeichen)."),
    "contact_off": ("The contact form is not set up yet.", "Das Kontaktformular ist noch nicht eingerichtet."),
    "early_needed": ("Early access comes with Simple Mods Pro.", "Frühzugang gehört zu Simple Mods Pro."),
    # mails
    "mail_verify_subject": ("Confirm your Simple Mods account", "Bestätige dein Simple-Mods-Konto"),
    "mail_verify_body": ("Hello,\n\nplease confirm your e-mail address for your Simple Mods account:\n\n{link}\n\nThe link works for 24 hours. "
                         "If you did not register, ignore this mail.\n\nSimple Mods",
                         "Hallo,\n\nbitte bestätige deine E-Mail-Adresse für dein Simple-Mods-Konto:\n\n{link}\n\nDer Link gilt 24 Stunden. "
                         "Wenn du dich nicht registriert hast, ignoriere diese Mail.\n\nSimple Mods"),
    "mail_reset_subject": ("Reset your Simple Mods password", "Simple-Mods-Passwort zurücksetzen"),
    "mail_reset_body": ("Hello,\n\nset a new password for your Simple Mods account here:\n\n{link}\n\nThe link works for one hour. "
                        "If you did not ask for it, ignore this mail - your password stays as it is.\n\nSimple Mods",
                        "Hallo,\n\nhier kannst du ein neues Passwort für dein Simple-Mods-Konto setzen:\n\n{link}\n\nDer Link gilt eine Stunde. "
                        "Wenn du das nicht angefordert hast, ignoriere diese Mail – dein Passwort bleibt, wie es ist.\n\nSimple Mods"),
    "mail_exists_subject": ("Your Simple Mods account", "Dein Simple-Mods-Konto"),
    "mail_exists_body": ("Hello,\n\nsomeone tried to register with this address, but it already has an account. "
                         "Forgot your password? Reset it here:\n\n{link}\n\nSimple Mods",
                         "Hallo,\n\njemand wollte sich mit dieser Adresse registrieren, sie hat aber schon ein Konto. "
                         "Passwort vergessen? Hier zurücksetzen:\n\n{link}\n\nSimple Mods"),
}


def t(lang: str, key: str, **params) -> str:
    en, de = TEXTS.get(key, (key, key))
    text = de if lang == "de" else en
    try:
        return text.format(**params)
    except (KeyError, IndexError, ValueError):
        return text
