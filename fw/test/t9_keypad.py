import threading
import time
import sys

test_word = "actual"

class Keypad():
    def __init__(self, keys):
        self.pressed_keys = set()
        threading.Thread(target=self.scripted_keys).start()

    def scripted_keys(self):
        time.sleep(3)
        for c in test_word:
            k = key_map[c]
            self.pressed_keys.add(k)
            time.sleep(0.5)
            self.pressed_keys.remove(k)
            time.sleep(0.5)

key_map = { 
    'b' : '2',
    'a' : '2',
    'c' : '2',
    'ç' : '2',
    'á' : '2',
    'd' : '3',
    'e' : '3',
    'é' : '3',
    'f' : '3',
    'g' : '4',
    'h' : '4',
    'i' : '4',
    'í' : '4',
    'j' : '5',
    'k' : '5',
    'l' : '5',
    'm' : '6',
    'n' : '6',
    'o' : '6',
    'ó' : '6',
    'p' : '7',
    'q' : '7',
    'r' : '7',
    's' : '7',
    't' : '8',
    'u' : '8',
    'ü' : '8',
    'ú' : '8',
    'v' : '8',
    'w' : '9',
    'x' : '9',
    'y' : '9',
    'z' : '9'
    }