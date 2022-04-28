class Keyboard():
    def __init__(self):
        self.buffer = ""

    def press(self, key):
        if key == Keycode.BACKSPACE:
            self.buffer = self.buffer[:-1]

    def release_all(self):
        pass

    def write(self, text):
        self.buffer = self.buffer + text
        # pynput virtual typing doesn't seem to work on Mavericks, so just print the output
        print("buffer:", self.buffer)

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