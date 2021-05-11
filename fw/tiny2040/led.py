import board
from digitalio import DigitalInOut
from digitalio import Direction

class Led():
    def __init__(self):
        self.leds = [DigitalInOut(board.LED_R), DigitalInOut(board.LED_G), DigitalInOut(board.LED_B)]
        for led in self.leds:    
            led.direction = Direction.OUTPUT
            led.value = True
    
    def show_red(self):
        self.leds[0].value = False

    def show_green(self):
        self.leds[1].value = False

    def show_blue(self):
        self.leds[2].value = False