from pynput import keyboard

class Keypad():
    def __init__(self, keys):
        self.keys = keys
        self.pressed_keys = set()
        listener = keyboard.Listener(
            on_press=lambda e: self.on_press(e),
            on_release=lambda e: self.on_release(e))
        listener.start()

    def on_press(self, key):
        if hasattr(key, 'char'):
            self.pressed_keys.add(key.char)

    def on_release(self, key):
        if hasattr(key, 'char'):
            self.pressed_keys.remove(key.char)