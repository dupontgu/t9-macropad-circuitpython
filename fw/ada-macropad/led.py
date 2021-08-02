import board
import neopixel

class Led():
    def __init__(self):
        self.pixel = neopixel.NeoPixel(board.NEOPIXEL, 12)
    
    def show_red(self):
        self.pixel.fill((150, 0, 0))

    def show_green(self):
        self.pixel.fill((0, 150, 0))

    def show_blue(self):
        self.pixel.fill((0, 0, 150))
