from main import speak, answer

print("PRANASETU AI DEMO")
print("Type a visitor question. Type exit to quit.")

while True:
    q = input("\nVisitor: ")
    if q.lower() in ("exit", "quit"):
        break
    speak(answer(q))
