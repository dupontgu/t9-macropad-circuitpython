from pynput import keyboard

class Keypad():
    def __init__(self, keys):
        self.keys = keys
        self.pressed_keys = set()
        self.listener = keyboard.Listener(
            suppress=True,
            on_press=lambda e: self.on_press(e),
            on_release=lambda e: self.on_release(e))
        self.listener.start()

    def on_press(self, key):
        if (key == keyboard.Key.esc):
            print("stopping key listener")
            self.listener.stop()
        if hasattr(key, 'char'):
            self.pressed_keys.add(key.char)

    def on_release(self, key):
        if hasattr(key, 'char'):
            self.pressed_keys.remove(key.char)