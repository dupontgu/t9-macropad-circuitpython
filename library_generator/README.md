# T9 Dictionary Generator

This Kotlin program takes in a list of words and spits out a binary file containing
a serialized Trie that can be traversed by this project's [firmware](../fw).

## Editing the dictionary

- Add/remove words from the [`dict.txt`](/src/main/resources/dict.txt) file. 
- All words should be separated by a newline character.
- words should be lowercase, and only contain the characters `a-z`
- if you want to use an entirely different file, be sure to drop it in the [resources](/src/main/resources)
directory, and change the `DICT_FILE` property in [Main.kt](/src/main/kotlin/Main.kt) to point to it.

## Loading the dictionary
- Run [`main()`](/src/main/kotlin/Main.kt).
- Copy the `out.bin` file on to your CIRCUITPY drive, next to the firmware (code.py). Be sure to make a backup of the old one if you don't want to lose it!