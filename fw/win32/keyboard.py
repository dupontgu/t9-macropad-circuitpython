from pynput.keyboard import Key, Controller

class Keyboard():
    def __init__(self):
        self.keyboard = Controller()
        self.pressed = []

    def press(self, key):
        mapped_key = key_mapping.get(key, None)
        if mapped_key is None:
            return
        if mapped_key not in self.pressed:
            self.pressed.append(mapped_key)
            self.keyboard.press(mapped_key)

    def release_all(self):
        for key in self.pressed:
            self.keyboard.release(key)
            self.pressed.remove(key)

    def write(self, text):
        self.keyboard.type(text)

class Keycode():
    BACKSPACE = "backspace"
    F1 = "F1"
    F2 = "F2"
    F3 = "F3"
    F4 = "F4"
    F5 = "F5"
    F6 = "F6"
    F7 = "F7"
    F8 = "F8"
    F9 = "F9"
    F10 = "F10"
    F11 = "F11"
    F12 = "F12"

key_mapping = {
    Keycode.BACKSPACE : Key.backspace
}