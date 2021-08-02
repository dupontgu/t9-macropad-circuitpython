import digitalio
import board
from digitalio import DigitalInOut
from digitalio import Direction
import adafruit_matrixkeypad

class Keypad():
    def __init__(self, keys):
        cols = [DigitalInOut(x) for x in (board.D10, board.D9, board.D8)]
        rows = [DigitalInOut(x) for x in (board.A3, board.D4, board.D5, board.D6)]
        self.keypad = adafruit_matrixkeypad.Matrix_Keypad(rows, cols, keys)

    @property
    def pressed_keys(self):
        return self.keypad.pressed_keys