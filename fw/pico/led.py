import board
from digitalio import DigitalInOut
from digitalio import Direction

class Led():
    def __init__(self):
        self.led = DigitalInOut(board.LED)
        self.led.direction = Direction.OUTPUT
        self.led.value = False
    
    def show_red(self):
        self.led.value = True

    def show_green(self):
        self.led.value = True

    def show_blue(self):
        self.led.value = True