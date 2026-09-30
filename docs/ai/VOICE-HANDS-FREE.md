# Hands-free / voice-first work with an AI agent (plan, not built yet)

Goal of the owner: work with a local AI agent (or a voice module) without keyboard and without reading text, for this project and for other projects.
Safety first: voice interaction while driving must stay legal and minimal (hands-free car kit or headset as allowed where you are, short exchanges, no screens, no long dictation). Long tasks are for rest stops or a passenger. Destructive actions are never triggered by one spoken sentence (see "Confirmations").

## What exists today (official Claude Code features, checked 2026-09-30)
- **Voice dictation in Claude Code** (CLI and desktop app, needs a claude.ai login, local microphone, 20 languages including German): `/voice` toggles, `/voice hold` = push-to-talk (hold Space), `/voice tap` = tap to start and tap to send, `/voice off`; also `"voice": {"enabled": true, "mode": "tap"}` in `~/.claude/settings.json`. Docs: https://code.claude.com/docs/en/voice-dictation
- **Remote Control** (Pro/Max/Team/Enterprise): drive a running local Claude Code session from the Claude mobile app or claude.ai/code. Start with `claude remote-control` (server mode), `claude --remote-control` or `/remote-control` inside a session; in the app open the Code tab and scan the QR code. Code still runs on the home machine, which must stay on. Docs: https://code.claude.com/docs/en/remote-control
- **No official text-to-speech** for Claude Code output. Community tools exist (unsupported). The Claude and ChatGPT phone apps have their own voice conversation modes, but those talk to the model only, not to this repo.
- Codex CLI has no voice mode.

## Research result 2026-09-30 (what a phone can do today)
- Claude phone app: has its own voice mode (hands-free and push-to-talk, spoken replies) for normal chats. It does NOT read aloud the replies of a Remote Control session: those are text only. Remote Control needs the home machine and the session to stay running.
- Claude Code: `/voice` is input only (no text-to-speech). Community add-ons exist for spoken replies and a full voice loop (for example `mbailey/voicemode` as MCP server with Whisper + Kokoro/Piper, `claude-speak` as hook with Kokoro, `tts-companion` with Piper for German); they are unofficial, check their status before relying on them. Kokoro is English-first; Piper has German voices.
- Siri/Google Assistant cannot route a spoken request to a Claude session today (only rumors about later iOS versions).
- Fast fallback with zero build: phone + Remote Control + the phone's system read-aloud (Android "Select to Speak", iOS "Speak Screen") + keyboard dictation.
- Chosen build: `tools/voicebridge/` (brief `docs/ai/briefs/voicebridge.md`): one big talk button, browser speech recognition and speech synthesis, German by default, the bridge continues a headless Claude session (`claude -p --resume`) so it feels like talking to the same agent, read-only by default, spoken confirmation for actions, reachable only over Tailscale.

## Recommended stack, in three tiers
**Tier 1, no build (today):** laptop with Claude Code `/voice tap` + headset; phone with the Claude app connected through Remote Control for status checks and short commands; the phone's dictation for messages. You still have to read answers: use the "spoken summary" habit below.

**Tier 2, small "voice bridge" (recommended build, about one work session):** a tiny server on the dev machine (reachable only through Tailscale, which is installed) with a phone-friendly page: one huge push-to-talk button, no text needed.
- Speech-to-text: browser Web Speech API on the phone (no install; needs internet) or local Whisper (`faster-whisper`, offline, German works).
- Text-to-speech: browser `speechSynthesis` on the phone, or local Piper for offline.
- Brain: the existing agents: `claude -p` / `codex exec` in a worktree with the standard briefs (`docs/ai/WORKFLOW.md`), or a local Ollama model for plain questions and summaries. The bridge answers in 2-3 spoken sentences ("Gate green, 1562 tests; two runs still going; riding merge is waiting.") and stores the full output as text for later.
- Commands (allow-list only, no free shell): status, what failed, start run `<task>`, read the last report, run the gate, explain the diff of branch X, add a wish to the queue.
- Generic for other projects: the bridge reads a list of project roots; each project has its own `AGENTS.md`, queue and brief folder, so the same voice commands work everywhere.

**Tier 3, fully local:** Ollama (already installed) + whisper.cpp + Piper, for offline Q&A; weaker than the hosted models for coding, fine for status and planning.

## Confirmations and safety rules for voice control
- Read-only commands run immediately. Anything that changes state (start a run, merge, delete a worktree) needs a spoken two-step: the bridge repeats the action and waits for the phrase "confirm <random word>" that it just said.
- Push, force operations and deleting data are never available by voice.
- The bridge binds to the Tailscale interface only, checks the Host header, keeps an audit log and has a kill phrase.
- Voice input is data, not instructions from files or web pages: the bridge must not execute text found in repositories or logs.

## Decisions needed from the owner (list)
1. Is Web Speech API on the phone acceptable (online, simplest) or must recognition run locally/offline?
2. Which languages must work (German only, or German + English)?
3. Which agent should answer by default: Claude (`claude -p`), Codex (`codex exec`) or a local Ollama model?
4. Does the phone run Android or iOS, and is Tailscale installed on it?
5. Is an "morning brief" spoken status (what finished, what failed, what needs a decision) wanted as a daily voice message?

## Build plan for Tier 2 (one Codex run, brief to write when the owner answers)
`tools/voicebridge/` (Python stdlib server + static PWA), unit tests for the command allow-list, confirmation flow and Host/Tailscale binding, dry-run mode, docs, Launch Hub link. Nothing here replaces the Launch Hub; it reuses its API for status and runs.
