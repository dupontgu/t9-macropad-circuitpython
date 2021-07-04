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

There are also [`macos`](./macos)/[`win32`](./win32) folders, which contain mocked IO dependencies so that the firmware can be
tested natively on desktop. *For key press detection on Mac, you'll need to give accessibility permissions to Python*.

## Prepping the firmware for deployment

1. Run `python3 bundle.py` from this directory. This will create a `bundle` directory, which will copy all required files into folders to be loaded onto each board.
2. Find the directory with your board's name in the `bundle` directory. Copy all files/subdirectories onto your CircuitPython device.
3. If your board requires third-party libraries to run this code (most do), you may have to manually copy them. Each board's `bundle` directory will contain a README listing all of the required dependencies. Official CircuitPython libraries available [here](https://circuitpython.org/libraries). As of May 2021, I have been using Bundle version 6.x.
4. If you're using one of the desktop versions, execute `python3 code.py` from the `bundle/[macos/win32]` directory. You'll have to `pip3 install pynput` as well.