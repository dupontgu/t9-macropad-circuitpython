import time
from led import Led
from keypad import Keypad
from keyboard import Keyboard
from keyboard import Keycode

NO_WORD = 0
PARTIAL_WORD = 1
WORD = 2

CACHE_SIZE = 8000

location_cache = { }
file_cache = [-1 for i in range(CACHE_SIZE)] 

leds = Led()

key_queue = []
held_keys = []
current_word = ""
written_word = ""

keys = (('1', '2', '3'),
        ('4', '5', '6'),
        ('7', '8', '9'), 
        ('*', '0', '#'))
keypad = Keypad(keys)

time.sleep(1)  # Sleep for a bit to avoid a race condition on some systems
keyboard = Keyboard()
time.sleep(0.5) 

def run_macro_mode():
    macro_map = {
        '1' : Keycode.F1,
        '2' : Keycode.F2,
        '3' : Keycode.F3,
        '4' : Keycode.F4,
        '5' : Keycode.F5,
        '6' : Keycode.F6,
        '7' : Keycode.F7,
        '8' : Keycode.F8,
        '9' : Keycode.F8,
        '0' : Keycode.F10,
        '#' : Keycode.F11,
        '*' : Keycode.F12
    }

    while True:
        initial_keys = keypad.pressed_keys
        for k in initial_keys:
            if k not in held_keys:
                held_keys.append(k)
                keyboard.press(macro_map[k])
        for hk in held_keys:
            if hk not in initial_keys:
                held_keys.remove(hk)
        keyboard.release_all()
    time.sleep(0.001)

def run_numeric_mode():
    while True:
        initial_keys = keypad.pressed_keys
        for k in initial_keys:
            if k not in held_keys:
                held_keys.append(k)
                keyboard.write(k)
        for hk in held_keys:
            if hk not in initial_keys:
                held_keys.remove(hk)
    time.sleep(0.001)

class Results():
    __slots__ = ['words', 'pres']
    def __init__(self, words, pres):
        self.words = words
        self.pres = pres

    def __str__(self):
        return f'words: {self.words}, pres: {self.pres}'

if ('1' in keypad.pressed_keys):
    leds.show_red()
    run_numeric_mode()
elif ('2' in keypad.pressed_keys):
    leds.show_green()
    run_macro_mode()
else:
    leds.show_blue()

force_break_word = False
 
# led = digitalio.DigitalInOut(board.LED)
# led.direction = digitalio.Direction.OUTPUT

keypad_dict = {
    '1' : ['1'],
    '2' : ['a', 'b', 'c'],
    '3' : ['d', 'e', 'f'],
    '4' : ['g', 'h', 'i'],
    '5' : ['j', 'k', 'l'],
    '6' : ['m', 'n', 'o'],
    '7' : ['p', 'q', 'r', 's'],
    '8' : ['t', 'u', 'v'],
    '9' : ['w', 'x', 'y', 'z'],
    '0' : [' ', '0', '\n'],
    '#' : ['.', ',', '?', '!']
}

def read_int(file, offset):
    if (offset < CACHE_SIZE):
        cached = file_cache[offset]
        if cached >= 0:
            return cached
    file.seek(offset * 3)
    x = int.from_bytes(file.read(3), 'big')
    if (offset < CACHE_SIZE):
        file_cache[offset] = x
    return x


def search(file, offset, s: str):
    poll_keys()
    if len(s) == 0:
        file_val = read_int(file, offset)
        return WORD if file_val == 1 else PARTIAL_WORD
    else:
        ch = ord(s[0]) - ord('a')
        file_val = read_int(file, offset + 1 + ch)
        if file_val == 0xFFFFFF:
            return WORD if len(s) == 1 else NO_WORD
        elif file_val > 0:
            return search(file, file_val, s[1:])
        return NO_WORD

def get_words(file, input, valid_prefixes):
    chars = keypad_dict[input]
    output_words = []
    output_prefixes = []
    for prefix in valid_prefixes:
        for char in chars:
            test_word = prefix + char
            result = search(file, 0, test_word)
            if result == PARTIAL_WORD:
                output_prefixes.append(test_word)
            elif result == WORD:
                output_words.append(test_word)
    return Results(output_words, output_prefixes)

def common_chars(old_word, new_word):
    old_len = len(old_word)
    common_count = old_len
    for i in range(min(old_len, len(new_word))):
        if old_word[i] is new_word[i]:
            common_count -= 1
        else:
            break
    return common_count


def submit(word):
    global current_word, written_word, key_queue
    current_word = word
    if len(key_queue) == 0:
        cc = common_chars(written_word, current_word)
        erase_num(cc)
        lw = len(written_word)
        written_word = current_word
        keyboard.write(current_word[(lw - cc):])

def erase_num(num):
    for _ in range(num):
        keyboard.press(Keycode.BACKSPACE)
        keyboard.release_all()

def emit_char(word, clear = True):
    global written_word, current_word
    if (written_word != current_word):
        submit(current_word)
    if clear:
        written_word = ""
        current_word = ""
    else:
        written_word += word
        current_word += word
    keyboard.write(word)

held_modified_keys = []
held_key_times = {}

last_modified_key = None
last_modified_time = time.monotonic()
last_modified_counter = 0

def flush_last_modified():
    global last_modified_counter, last_modified_time, last_modified_key
    last_modified_key = None
    last_modified_time = time.monotonic()
    last_modified_counter = 0

def poll_keys_modified(current_keys):
    global force_break_word, last_modified_key, last_modified_time, last_modified_counter
    for k in current_keys:
        if k not in held_modified_keys and k != '*':
            force_break_word = True
            held_modified_keys.append(k)
            now = time.monotonic()
            key_dict = keypad_dict[k]
            if k is not last_modified_key or (now - last_modified_time > 0.6):
                flush_last_modified()
                emit_char(key_dict[0])
                last_modified_key = k
            else:
                last_modified_counter += 1
                num_keys_for_char = len(key_dict)
                if last_modified_counter < num_keys_for_char:
                    erase_num(1)
                    emit_char(key_dict[last_modified_counter])
                    last_modified_time = now
                elif last_modified_counter == num_keys_for_char:
                    erase_num(1)
                    emit_char(k)
                    flush_last_modified()

    for hk in held_modified_keys:
        if hk not in current_keys:
            held_modified_keys.remove(hk)

def poll_keys():
    initial_keys = keypad.pressed_keys
    if '*' in initial_keys:
        return poll_keys_modified(initial_keys)
    else:
        flush_last_modified()
    for k in initial_keys:
        if k not in held_keys:
            held_keys.append(k)
            held_key_times[k] = time.monotonic()
            if k != '#':
                key_queue.append(k)
        elif k == '#':
            now = time.monotonic()
            pound_held = now - held_key_times.get('#', now)
            if pound_held > 0.5:
                key_queue.append('*')
                held_keys.remove(k)
    for hk in held_keys:
        if hk not in initial_keys:
            held_keys.remove(hk)
            if hk == '#':
                key_queue.append(hk)

with open("out.bin", "rb") as fp:
    while True:
        word_index = 0
        current_word = ""
        prefixes = [""]
        results = [ Results([""], []) ]
        result_index = 0
        while True:
            if force_break_word:
                force_break_word = False
                break
            if (len(key_queue) == 0):
                poll_keys()
                time.sleep(0.001)
                continue
            c = key_queue.pop(0)
            if c == '0':
                emit_char(" ")
                break
            if c == '*':
                emit_char("\n")
                break
            elif c == '#':
                result = results[result_index]
                word_count = len(result.words)
                pres_count = len(result.pres)
                if word_count > 0:
                    word_index = (word_index + 1) % word_count
                    submit(result.words[word_index])
                elif pres_count > 0:
                    word_index = (word_index + 1) % pres_count
                    submit(result.pres[word_index])
                continue
            if c == '1':
                result_index = max(0, result_index - 1)
                result = results[result_index]
                if (len(results) > 1):
                    results.pop()
                else:
                    erase_num(1)
            elif c < '2' or c > '9':
                break
            else:
                result = get_words(fp, c, prefixes)
                results.append(result)
                result_index += 1
            print(result)
            word_count = len(result.words)
            pres_count = len(result.pres)
            if word_count > 0:
                submit(result.words[word_index % word_count])
            elif pres_count > 0:
                submit(result.pres[word_index % pres_count])
            elif c >= '2' and c <= '9':
                emit_char(c, False)

            prefixes = result.words + result.pres


 