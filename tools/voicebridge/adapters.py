"""Optional local speech adapters. No optional import occurs in the default MVP."""
from pathlib import Path
import shutil
import subprocess
import tempfile


class StubSTT:
    def transcribe(self, audio, language):
        raise ValueError("Server-STT ist ausgeschaltet. Browser-Spracherkennung verwenden.")


class WhisperSTT:
    def __init__(self, model="small"):
        from faster_whisper import WhisperModel
        self.model = WhisperModel(model, device="cpu", compute_type="int8")

    def transcribe(self, audio, language):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / "audio.webm"
            path.write_bytes(audio)
            segments, _ = self.model.transcribe(str(path), language=language.split("-")[0])
            return " ".join(segment.text for segment in segments)


class StubTTS:
    def synthesize(self, text):
        raise ValueError("Server-TTS ist ausgeschaltet. Browser-Stimme verwenden.")


class PiperTTS:
    def __init__(self, model):
        self.executable = shutil.which("piper")
        self.model = Path(model).resolve()
        if not self.executable or not self.model.is_file():
            raise ValueError("Piper executable/model missing")

    def synthesize(self, text):
        with tempfile.TemporaryDirectory() as folder:
            output = Path(folder) / "speech.wav"
            subprocess.run([self.executable, "--model", str(self.model), "--output_file", str(output)],
                           input=text, text=True, check=True, timeout=60, capture_output=True, shell=False)
            return output.read_bytes()
