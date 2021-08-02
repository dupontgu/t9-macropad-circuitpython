
import board
import keypad

class Keypad():
    def __init__(self, keys):
        self._key_pins = [getattr(board, "KEY%d" % (num + 1)) for num in list(range(12))]
        self._keys = keypad.Keys(self._key_pins, value_when_pressed=False, pull=True)
        self._key_map = [item for sublist in list(keys) for item in sublist]
        self._held_keys = []

    @property
    def pressed_keys(self):
        current = self._keys.events.get()
        while current is not None:
            k = self._key_map[current.key_number]
            if current.pressed and k not in self._held_keys:
                self._held_keys.append(k)
            elif current.released and k in self._held_keys:
                self._held_keys.remove(k)
            current = self._keys.events.get()
        return self._held_keys