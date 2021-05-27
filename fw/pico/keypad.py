import digitalio
import board
from digitalio import DigitalInOut
from digitalio import Direction
import adafruit_matrixkeypad

class Keypad():
    def __init__(self, keys):
        cols = [digitalio.DigitalInOut(x) for x in (board.GP11, board.GP10, board.GP9)]
        rows = [digitalio.DigitalInOut(x) for x in (board.GP12, board.GP13, board.GP14, board.GP15)]
        self.keypad = adafruit_matrixkeypad.Matrix_Keypad(rows, cols, keys)

    @property
    def pressed_keys(self):
        return self.keypad.pressed_keys