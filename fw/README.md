### Check out the official project website [here](https://t9-library-generator.uk.r.appspot.com/)
# Firmware
This code _should_ run on any CircuitPython-enabled board, but is most useful when there is >= 4MB of flash.
As of May 2021, it's only been tested on various RP2040 dev boards.

Board-agnostic code lives in the [core](./core) directory. 

Board-specific code - for things like driving indicator LEDs, GPIO mappings - are in other folders. 
Word libraries are also in these individual directories for now, as certain dev boards have less flash space.
Those boards have smaller default libraries.
Currently supported boards:
* [Pimoroni Tiny2040](./tiny2040)
* [Raspberry Pi Pico](./pico)
* [Adafruit QT Py RP2040](./qtpy2040)
* [Adafruit MacroPad RP2040](./ada-macropad)

There are also [`macos`](./macos)/[`win32`](./win32) folders, which contain mocked IO dependencies so that the firmware can be
tested natively on desktop. *For key press detection on Mac, you'll need to give accessibility permissions to Python*.

## Prepping the firmware for deployment

1. Run `python3 bundle.py` from this directory. This will create a `bundle` directory, which will copy all required files into folders to be loaded onto each board.
2. Find the directory with your board's name in the `bundle` directory. Copy all files/subdirectories onto your CircuitPython device.
3. If your board requires third-party libraries to run this code (most do), you may have to manually copy them. Each board's `bundle` directory will contain a README listing all of the required dependencies. Official CircuitPython libraries available [here](https://circuitpython.org/libraries). As of May 2021, I have been using Bundle version 6.x.
4. If you're using one of the desktop versions, execute `python3 code.py` from the `bundle/[macos/win32]` directory. You'll have to `pip3 install pynput` as well.

## Customizing the keypad layout

You can customize which letters get attached to each key by changing the `keypad_dict` dictionary in `code.py`. The default is:
```python
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
```
But as [Ben Torvaney discovered](https://torvaney.github.io/projects/t9-optimised.html), you can optimize your typing even further by using something like:
```python
keypad_dict = {
    '1' : ['1'],
    '2' : ['a', 'm', 'r'],
    '3' : ['c', 'd', 'f', 'p', 'u'],
    '4' : ['h', 'n', 'q', 't'],
    '5' : ['i', 'l', 'w', 'y'],
    '6' : ['b', 'e', 'g', 'v', 'x'],
    '7' : ['j', 'k', 'o', 's', 'z'],
    '8' : ['8'],
    '9' : ['9'],
    '0' : [' ', '0', '\n'],
    '#' : ['.', ',', '?', '!']
}
```

**Note that the keypad will not run unless all characters are accounted for!**