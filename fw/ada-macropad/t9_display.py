import board
from adafruit_simple_text_display import SimpleTextDisplay

class Display():
    def __init__(self):
        title=None
        title_scale=1
        title_length=80
        text_scale=2
        font=None
        self._text_lines = SimpleTextDisplay(
            title=title,
            title_color=SimpleTextDisplay.WHITE,
            title_scale=title_scale,
            title_length=title_length,
            text_scale=text_scale,
            font=font,
            colors=(SimpleTextDisplay.WHITE,),
            display=board.DISPLAY,
        )

    def display_results(self, result, index):
        arr = result.words if len(result.words) > 0 else result.pres
        word_count = len(arr)
        if word_count > 0:
            # display can only display 2.5 lines
            for i in range(0, max(3, word_count)):
                adj_i = (i + index) % word_count
                w = arr[adj_i]
                # highlight the first line
                self._text_lines[i].text = ">"+w+"<" if i == 0 else w
        self._text_lines.show()
