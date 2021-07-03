import usb_hid
import adafruit_hid.keyboard
from adafruit_hid.keyboard_layout_us import KeyboardLayoutUS
import adafruit_hid.keycode

class Keyboard():
    def __init__(self):
        self.keyboard = adafruit_hid.keyboard.Keyboard(usb_hid.devices)
        self.keyboard_layout = KeyboardLayoutUS(self.keyboard)

    def press(self, key):
        self.keyboard.press(key)

    def release_all(self):
        self.keyboard.release_all()

    def write(self, text):
        self.keyboard_layout.write(text)

Keycode = adafruit_hid.keycode.Keycode