from pynput import keyboard
import time

class Keypad():
    def __init__(self, keys):
        global listener
        self.keys = keys
        self.pressed_keys = set()
        listener = keyboard.Listener(
            suppress=True,
            on_press=lambda e: self.on_press(e),
            on_release=lambda e: self.on_release(e),
            win32_event_filter= filter)
        listener.start()

    def on_press(self, key):
        global listener
        if (key == keyboard.Key.esc):
            listener.stop()
        if hasattr(key, 'char'):
            self.pressed_keys.add(key.char)

    def on_release(self, key):
        if hasattr(key, 'char') and key.char in self.pressed_keys:
            # sleep a bit for naughty keyboards that release too quickly
            time.sleep(0.05)
            self.pressed_keys.remove(key.char)

def filter(msg, data):
    # only capture number presses
    # TODO add modifier keys
    if 48 <= data.vkCode <= 57:
        listener._suppress = True
        return True
    else :
        listener._suppress = False
        return False
    