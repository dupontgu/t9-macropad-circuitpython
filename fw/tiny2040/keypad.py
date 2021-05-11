import digitalio
import board
from digitalio import DigitalInOut
from digitalio import Direction
import adafruit_matrixkeypad

class Keypad():
    def __init__(self, keys):
        cols = [DigitalInOut(x) for x in (board.GP3, board.GP4, board.GP5)]
        rows = [DigitalInOut(x) for x in (board.GP29_A3, board.GP28_A2, board.GP27_A1, board.GP26_A0)]
        self.keypad = adafruit_matrixkeypad.Matrix_Keypad(rows, cols, keys)

    @property
    def pressed_keys(self):
        return self.keypad.pressed_keys