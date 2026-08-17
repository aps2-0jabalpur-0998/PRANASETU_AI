import json
import time
import threading
import re

try:
    import serial
except ImportError:
    serial = None

import pyttsx3
import speech_recognition as sr
import requests

from config import SERIAL_PORT, BAUD_RATE, USE_OLLAMA, OLLAMA_URL, OLLAMA_MODEL, LANGUAGE, VOICE_RATE

with open("knowledge.json", "r", encoding="utf-8") as f:
    KNOWLEDGE = json.load(f)

state = {
    "intruder": False,
    "fire": False,
    "blue_carbon": "UNKNOWN",
    "algae": "UNKNOWN"
}

engine = pyttsx3.init()
engine.setProperty("rate", VOICE_RATE)

def speak(text):
    print("PRANASETU AI:", text)
    engine.say(text)
    engine.runAndWait()

def serial_reader():
    if serial is None:
        print("pyserial not installed; demo mode.")
        return

    try:
        ser = serial.Serial(SERIAL_PORT, BAUD_RATE, timeout=1)
        print("Arduino connected:", SERIAL_PORT)
    except Exception as e:
        print("Arduino not connected:", e)
        return

    last_fire = False
    last_intruder = False

    while True:
        try:
            line = ser.readline().decode(errors="ignore").strip()
            if not line or ":" not in line:
                continue

            key, value = line.split(":", 1)
            key = key.strip().upper()
            value = value.strip()

            if key == "INTRUDER":
                state["intruder"] = value == "1"
                if state["intruder"] and not last_intruder:
                    speak("Warning. Unauthorized movement has been detected.")
                last_intruder = state["intruder"]

            elif key == "FIRE":
                state["fire"] = value == "1"
                if state["fire"] and not last_fire:
                    speak("Emergency alert. Fire has been detected. Please follow the safety procedure.")
                last_fire = state["fire"]

            elif key == "BLUE_CARBON":
                state["blue_carbon"] = value

            elif key == "ALGAE":
                state["algae"] = value

        except Exception as e:
            print("Serial error:", e)
            time.sleep(1)

def status_answer():
    return (
        f"Current system status: Intruder is {'active' if state['intruder'] else 'normal'}, "
        f"fire status is {'alert' if state['fire'] else 'normal'}, "
        f"blue carbon system is {state['blue_carbon']}, "
        f"and green algae tree status is {state['algae']}."
    )

def local_answer(question):
    q = question.lower().strip()

    if any(x in q for x in ["status", "system status", "what is happening", "current condition"]):
        return status_answer()

    faq = KNOWLEDGE.get("faq", {})
    for key, answer in faq.items():
        if key in q:
            return answer

    modules = KNOWLEDGE["modules"]
    if "intruder" in q or "security" in q:
        return modules["intruder"]
    if "fire" in q or "smoke" in q or "flame" in q:
        return modules["fire"]
    if "blue carbon" in q or "carbon" in q:
        return modules["blue_carbon"]
    if "algae" in q or "green algae" in q:
        return modules["green_algae_tree"]

    return None

def ollama_answer(question):
    if not USE_OLLAMA:
        return None

    context = json.dumps(KNOWLEDGE, ensure_ascii=False)
    prompt = f"""You are PranaSetu AI, a science-exhibition voice assistant.
Answer visitors clearly, briefly and accurately.
You know only the project context below plus the live sensor status.
Do not invent sensor readings or claim that the project performs something it does not.
If a question is unrelated to the project, politely say you are designed mainly for PranaSetu.

PROJECT KNOWLEDGE:
{context}

LIVE STATUS:
{status_answer()}

VISITOR QUESTION:
{question}
"""

    try:
        r = requests.post(
            OLLAMA_URL,
            json={"model": OLLAMA_MODEL, "prompt": prompt, "stream": False},
            timeout=20
        )
        r.raise_for_status()
        return r.json().get("response", "").strip()
    except Exception as e:
        print("Ollama unavailable:", e)
        return None

def answer(question):
    result = local_answer(question)
    if result:
        return result

    result = ollama_answer(question)
    if result:
        return result

    return "I am the PranaSetu AI assistant. You can ask me about the intruder system, fire detection, blue carbon, or the green algae tree."

def listen():
    recognizer = sr.Recognizer()

    with sr.Microphone() as source:
        print("\nListening...")
        recognizer.adjust_for_ambient_noise(source, duration=0.5)
        try:
            audio = recognizer.listen(source, timeout=5, phrase_time_limit=8)
        except sr.WaitTimeoutError:
            return None

    try:
        text = recognizer.recognize_google(audio, language=LANGUAGE)
        print("YOU:", text)
        return text
    except Exception:
        return None

def main():
    threading.Thread(target=serial_reader, daemon=True).start()

    speak("Hello. I am PranaSetu AI. Ask me anything about this project.")

    while True:
        question = listen()
        if not question:
            continue

        if re.search(r"\b(exit|quit|stop|goodbye)\b", question.lower()):
            speak("Goodbye. Thank you for visiting PranaSetu.")
            break

        speak(answer(question))

if __name__ == "__main__":
    main()
