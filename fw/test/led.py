class Led():
    def __init__(self):
        self.color = None
    
    def show_red(self):
        print("set led to red")
        self.color = 'r'

    def show_green(self):
        print("set led to green")
        self.color = 'g'

    def show_blue(self):
        print("set led to blue")
        self.color = 'b'